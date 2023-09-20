package io.github.mattidragon.jsonpatch.lang.parse;

import io.github.mattidragon.jsonpatch.lang.runtime.Program;

import java.util.List;

public sealed interface ParseResult {
    record Success(Program program) implements ParseResult {}
    record Fail(List<Parser.ParseException> errors) implements ParseResult {}
}
