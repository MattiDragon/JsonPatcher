package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.runtime.Program;

import java.util.List;

public sealed interface ParseResult {
    record Success(Program program, PatchMetadata metadata) implements ParseResult {}
    record Fail(List<Parser.ParseException> errors) implements ParseResult {}
}
