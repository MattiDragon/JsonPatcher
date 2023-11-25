package io.github.mattidragon.jsonpatcher.patch;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatchStorage implements EvaluationContext.LibraryLocator {
    private static final ThreadLocal<ExecutorService> LIBRARY_APPLICATOR = ThreadLocal.withInitial(() -> Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("JsonPatch Library Builder (%s)").build()));

    private final Multimap<String, Patch> namespacePatches = LinkedHashMultimap.create();
    private final Multimap<String, Patch> pathPatches = LinkedHashMultimap.create();

    private final Multimap<String, Patch> namespaceFilteredPatches = LinkedHashMultimap.create();

    private final Multimap<Identifier, Patch> directIdPatches = LinkedHashMultimap.create();
    private final List<Patch> nonTrivialPatches = new ArrayList<>();
    private final List<Patch> alwaysActivePatches = new ArrayList<>();

    private final Map<Identifier, Patch> libraries = new HashMap<>();
    private final List<Patch> metaPatches = new ArrayList<>();

    /*
    Group 1: namespaced patches
    Group 2: non-trivial patches
    Group 3: direct id patches
    Group 4: path only patches
    Group 5: always active patches
    Group 6: namespace filtered patches

    |             |666222361425|
    +-------------+------------+
    |pathStartEnd | X  X  X  X |
    |path         |X  X  X  X  |
    |namespace    |XXX   XXX   |
    |regex        |xxxxxx      |
    */
    public PatchStorage(List<Patch> patches) {
        for (var patch : patches) {
            if (patch.isMeta()) metaPatches.add(patch);

            patch.target().forEach(target -> {
                var simplePath = target.path().map(PatchTarget.Path::path).flatMap(either -> either.left());
                var splitPath = target.path().map(PatchTarget.Path::path).flatMap(either -> either.right());

                // All regex patches and patches with split paths need to be checked at runtime. We can only bucket them by namespace here.
                if (target.regex().isPresent() || splitPath.isPresent()) {
                    if (target.namespace().isPresent()) {
                        namespaceFilteredPatches.put(target.namespace().get(), patch);
                    } else {
                        nonTrivialPatches.add(patch);
                    }
                    return;
                }

                // Full id patches will be somewhat command and thus receive their own bucket
                if (target.namespace().isPresent() && simplePath.isPresent()) {
                    var id = Identifier.tryParse(target.namespace().get() + ":" + simplePath.get());
                    if (id == null) return; // Invalid id, can't match anything

                    directIdPatches.put(id, patch);
                    return;
                }

                // Namespace and full path only patches are rare, but lookup is fast, so we can save a little bit of time by putting them in their own bucket
                if (target.namespace().isPresent()) {
                    namespacePatches.put(target.namespace().get(), patch);
                    return;
                }
                if (simplePath.isPresent()) {
                    pathPatches.put(simplePath.get(), patch);
                    return;
                }

                alwaysActivePatches.add(patch);
            });
            libraries.put(patch.id(), patch);
        }
    }

    public boolean hasPatches(Identifier id) {
        if (!alwaysActivePatches.isEmpty()) return true;
        if (namespacePatches.containsKey(id.getNamespace())) return true;
        if (pathPatches.containsKey(id.getPath())) return true;
        if (directIdPatches.containsKey(id)) return true;
        if (namespaceFilteredPatches.get(id.getNamespace()).stream().anyMatch(patch -> patch.target().stream().anyMatch(target -> target.test(id)))) return true;
        return nonTrivialPatches.stream().anyMatch(patch -> patch.target().stream().anyMatch(target -> target.test(id)));
    }

    public Collection<Patch> getPatches(Identifier id) {
        // Use a set to avoid duplicates from patches with multiple targets. Also allows us to not store which target put a patch in a bucket
        var patches = new HashSet<>(alwaysActivePatches);
        namespacePatches.entries().stream().filter(entry -> entry.getKey().equals(id.getNamespace())).map(Map.Entry::getValue).forEach(patches::add);
        pathPatches.entries().stream().filter(entry -> entry.getKey().equals(id.getPath())).map(Map.Entry::getValue).forEach(patches::add);
        directIdPatches.entries().stream().filter(entry -> entry.getKey().equals(id)).map(Map.Entry::getValue).forEach(patches::add);
        nonTrivialPatches.stream().filter(patch -> patch.target().stream().anyMatch(target -> target.test(id))).forEach(patches::add);
        return patches;
    }

    public Collection<Patch> getMetaPatches() {
        return metaPatches;
    }

    public int size() {
        return libraries.size();
    }

    @Override
    public void loadLibrary(String libraryName, Value.ObjectValue libraryObject, SourceSpan importPos) {
        var libId = Identifier.tryParse(libraryName);
        if (libId == null) {
            throw new EvaluationException("Invalid library name '%s'".formatted(libraryName), importPos);
        }

        var userLib = libraries.get(libId);
        if (userLib == null) {
            throw new EvaluationException("Cannot locate library '%s'".formatted(libraryName), importPos);
        }

        Patcher.runPatch(userLib, LIBRARY_APPLICATOR.get(), e -> {
            if (e instanceof EvaluationException evaluationException) {
                throw new EvaluationException("Failed to load library %s".formatted(libId), importPos, evaluationException);
            }
            throw new RuntimeException("Failed to load library %s".formatted(libId), e);
        }, this, libraryObject, Patcher.Settings.builder().library().build());
    }
}
