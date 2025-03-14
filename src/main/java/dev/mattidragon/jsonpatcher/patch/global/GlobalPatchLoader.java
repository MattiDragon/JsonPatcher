package dev.mattidragon.jsonpatcher.patch.global;

import com.google.common.base.Suppliers;
import dev.mattidragon.jsonpatcher.lang.ast.meta.MetadataKey;
import dev.mattidragon.jsonpatcher.lang.ast.meta.TreeMetadata;
import dev.mattidragon.jsonpatcher.lang.error.Diagnostic;
import dev.mattidragon.jsonpatcher.lang.error.DiagnosticsBuilder;
import dev.mattidragon.jsonpatcher.lang.parse.Lexer;
import dev.mattidragon.jsonpatcher.lang.parse.Parser;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.MetadataString;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.PatchMetadata;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.CompilationException;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.compiler.CompilerOptions;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.Library;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.LibraryGroup;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.ProgramData;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.patch.PatchLoader;
import dev.mattidragon.jsonpatcher.patch.PatchLoaderDiagnostic;
import dev.mattidragon.jsonpatcher.patch.Patcher;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GlobalPatchLoader {
    private static List<Library> globalLibs = new ArrayList<>();
    private static List<GlobalPatch> globalPatches = new ArrayList<>();

    private static List<GlobalPatchSource> findSources() {
        var sources = new ArrayList<GlobalPatchSource>();
        sources.add(new GlobalPatchSource( "scripts:global", JsonPatcher.DATA_DIR.resolve("scripts"), TrustLevel.MODPACK));
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            mod.findPath("jsonpatcher/scripts")
                    .ifPresent(path ->
                            sources.add(new GlobalPatchSource("mod:" + mod.getMetadata().getId(), path, TrustLevel.MOD)));
        }
        return sources;
    }

    public static synchronized void loadGlobalPatches() {
        // Allocating new lists keeps previous ones valid. This prevents threading issues
        globalLibs = new ArrayList<>();
        globalPatches = new ArrayList<>();

        var environment = new EvaluationEnvironment(CompilerOptions.DEFAULT);
        if (Config.MANAGER.get().dumpCompiledPatches()) {
            environment.enableDumping(JsonPatcher.DATA_DIR.resolve("dump").resolve("global"));
        }
        environment.enableLogging(v -> JsonPatcher.RELOAD_LOGGER.debug("Debug from global patch: {}", v));
        environment.bootstrap();

        for (var source : findSources()) {
            globalPatches.addAll(loadPatchDir(source, environment));
        }

        JsonPatcher.RELOAD_LOGGER.info("Loaded {} global patches. ({} libraries)", globalPatches.size(), getGlobalLibs().size());
    }

    // Synchronized to block access while reloading
    public static synchronized List<Library> getGlobalLibs() {
        return Collections.unmodifiableList(globalLibs);
    }

    // Synchronized to block access while reloading
    public static void runEntrypoint(GlobalPatch.Entrypoint entrypoint) {
        List<GlobalPatch> toRun;
        synchronized (GlobalPatchLoader.class) {
            toRun = globalPatches.stream()
                    .filter(globalPatch -> globalPatch.entrypoint() == entrypoint)
                    .toList();
        }

        var errors = new ArrayList<RuntimeException>();

        try (var executor = Executors.newSingleThreadExecutor()) {
            for (var patch : toRun) {
                Patcher.runPatch(patch, executor, errors::add, new Value.ObjectValue());
            }
        }

        if (!errors.isEmpty()) {
            var error = errors.getFirst();
            for (var e : errors.subList(1, errors.size())) {
                error.addSuppressed(e);
            }
            JsonPatcher.MAIN_LOGGER.error("Errors while running entrypoint patches for {}", entrypoint, error);
        }
    }

    private static List<GlobalPatch> loadPatchDir(GlobalPatchSource source, EvaluationEnvironment environment) {
        var patches = new ArrayList<GlobalPatch>();
        try (var stream = Files.walk(source.path())) {
            var files = stream.filter(Files::isRegularFile).toList();
            for (var file : files) {
                var id = source.idPrefix() + ":" + file.relativize(source.path())
                        .toString()
                        .replace(file.getFileSystem().getSeparator(), "/")
                        .replaceFirst("^\\./", "");
                var code = Files.readString(file);
                var patch = loadPath(id, code, source.trustLevel(), environment);
                if (patch == null) continue;
                patches.add(patch);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load global patches with prefix " + source.idPrefix(), e);
        }
        return patches;
    }

    private static @Nullable GlobalPatch loadPath(String id, String code, TrustLevel trust, EvaluationEnvironment environment) {
        var diagnosticsBuilder = new DiagnosticsBuilder();

        var lexResult = Lexer.lex(code, id, diagnosticsBuilder);
        var parseResult = Parser.parse(lexResult.tokens(), diagnosticsBuilder);

        var treeMeta = parseResult.treeMetadata();
        var meta = parseResult.metadata();
        if (meta.has("enabled") && !meta.getBoolean("enabled")) {
            return null;
        }

        var roles = new HashSet<String>();

        var priority = PatchLoader.getPriority(meta);
        var libraryMetadata = PatchLoader.getLibraryMetadata(diagnosticsBuilder, meta, treeMeta, roles);
        var entrypoint = getEntrypointMeta(meta, treeMeta, diagnosticsBuilder, roles);

        var className = "jsonpatcher_global/"
                        + id.replaceAll("[-.:]", "_");

        var builder = ProgramData.builder(parseResult)
                .scriptName(id)
                .className(className);

        if (trust.ordinal() >= Config.MANAGER.get().reflectionMinTrustLevel().ordinal()) {
            builder.allowLibraryGroup(LibraryGroup.REFLECTION);
        }

        EvaluationEnvironment.AddedProgram added;
        try {
            added = environment.addProgram(builder.build());
        } catch (CompilationException e) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    null,
                    e.getMessage(),
                    Diagnostic.Kind.ERROR,
                    20
            ));
            return null;
        }

        var diagnostics = diagnosticsBuilder.build();
        var errors = diagnostics.errors();
        if (!errors.isEmpty()) {
            JsonPatcher.MAIN_LOGGER.error("Errors while loading global patch {}:\n{}", id, errors.stream().map(Diagnostic::toDisplay).collect(Collectors.joining("\n")));
            return null;
        }
        var warnings = diagnostics.warnings();
        if (!warnings.isEmpty()) {
            JsonPatcher.MAIN_LOGGER.warn("Warnings while loading global patch {}:\n{}", id, warnings.stream().map(Diagnostic::toDisplay).collect(Collectors.joining("\n")));
        }

        if (libraryMetadata != null) {
            Supplier<Value.ObjectValue> supplier = () -> {
                var obj = new Value.ObjectValue();
                added.run(obj);
                return obj;
            };
            if (libraryMetadata.shared()) {
                supplier = Suppliers.memoize(supplier::get);
            }
            var lib = new Library(
                    LibraryGroup.DEFAULT,
                    id,
                    supplier
            );
            globalLibs.add(lib);
            environment.addLibrary(lib);
        }

        return new GlobalPatch(added, id, priority, trust, entrypoint);
    }

    private static @Nullable GlobalPatch.Entrypoint getEntrypointMeta(PatchMetadata meta, TreeMetadata treeMeta, DiagnosticsBuilder diagnosticsBuilder, Set<String> roles) {
        if (!meta.has("entrypoint")) return null;

        roles.add("entrypoint");

        if (!(meta.get("entrypoint") instanceof MetadataString(var string))) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    treeMeta.get(meta.get("entrypoint"), MetadataKey.MAIN_POS).orElse(null),
                    "Entrypoint must be set to string",
                    Diagnostic.Kind.ERROR,
                    21
            ));
            return null;
        }

        return switch (string) {
            case "main" -> GlobalPatch.Entrypoint.MAIN;
            case "client" -> GlobalPatch.Entrypoint.CLIENT;
            default -> {
                diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                        treeMeta.get(meta.get("entrypoint"), MetadataKey.MAIN_POS).orElse(null),
                        "Invalid entrypoint: " + string,
                        Diagnostic.Kind.ERROR,
                        22
                ));
                yield null;
            }
        };
    }

    public static void setupDirs() {
        try {
            setupScriptsDir();
            dumpDocsDir();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to setup jsonpatcher files", e);
        }
    }

    private static void setupScriptsDir() throws IOException {
        var scriptsDir = JsonPatcher.DATA_DIR.resolve("scripts");
        if (Files.exists(scriptsDir)) return;

        Files.createDirectories(scriptsDir);
        Files.writeString(scriptsDir.resolve("README.md"), """
                # JsonPatcher global scripts
                Any jsonpatcher scripts placed here will be loaded on startup.
                
                If a script has the `@library` meta tag it will be available to all patches
                from elsewhere with a prefix of `scripts::` for imports.
                
                If a script has the `@init "main";` meta tag it will be executed during mod init.
                If it has the `@init "client";` meta tag it will be executed during client mod init.
                
                Under default config scripts present here will have access to the reflection library,
                allowing them to interact with Java code freely.
                """);
        Files.writeString(scriptsDir.resolve("example.jsonpatch"), """
                @version "2";
                @init "main";
                
                import "reflection";
                
                val ResourceLocation = reflection.findClass("net.minecraft.resources.ResourceLocation");
                
                val id1 = ResourceLocation.withDefaultNamespace("test_id");
                val id2 = ResourceLocation.fromNamespaceAndPath("minecraft", "test_id");
                
                debug.assert(id1.equals(id2));
                """);
    }

    private static void dumpDocsDir() throws IOException {
        var docsDir = JsonPatcher.DATA_DIR.resolve("docs");
        Files.createDirectories(docsDir);
        Files.writeString(docsDir.resolve("README.md"), """
                # JsonPatcher doc files
                This directory will in the future contain docs and code for the standard library.
                """);
    }
}
