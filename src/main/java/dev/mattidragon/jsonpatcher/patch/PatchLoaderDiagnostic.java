package dev.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.ast.ProgramNode;
import dev.mattidragon.jsonpatcher.lang.ast.SourceSpan;
import dev.mattidragon.jsonpatcher.lang.error.Diagnostic;
import net.minecraft.util.annotation.FieldsAreNonnullByDefault;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
public record PatchLoaderDiagnostic(@Nullable SourceSpan pos, String message, Kind kind, int code) implements Diagnostic {
    @Override
    public @Nullable ProgramNode node() {
        return null;
    }

    @Override
    public String id() {
        return "LOADER-" + code;
    }
}
