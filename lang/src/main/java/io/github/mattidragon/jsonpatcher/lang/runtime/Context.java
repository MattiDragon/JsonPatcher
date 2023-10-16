package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;

public interface Context {
    Context withRoot(Value.ObjectValue root);

    Context newScope();

    Value findLibrary(String libraryName, SourceSpan pos);

    VariableStack variables();

    Value.ObjectValue root();
}
