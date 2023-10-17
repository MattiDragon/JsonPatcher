package io.github.mattidragon.jsonpatcher.patch;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record PatchTarget(
        Optional<String> namespace,
        Optional<Path> path,
        Optional<String> regex) implements Predicate<Identifier> {
    private static final Codec<PatchTarget> ID_CODEC = Identifier.CODEC
            .flatComapMap(id -> new PatchTarget(Optional.of(id.getNamespace()), Optional.of(new Path(Either.left(id.getPath()))), Optional.empty()),
                    target -> {
                        if (target.regex.isPresent())
                            return DataResult.error(() -> "Can't serialize to id form with regex");
                        if (target.namespace.isEmpty())
                            return DataResult.error(() -> "Can't serialize to id form without namespace");
                        if (target.path.isEmpty())
                            return DataResult.error(() -> "Can't serialize to id form without path");

                        var checkedPath = target.path.get().path.map(DataResult::success, pair -> DataResult.<String>error(() -> "Can't serialize split path to id form"));
                        return checkedPath.map(path -> new Identifier(target.namespace.get(), path));
                    });

    private static final Codec<PatchTarget> SPLIT_CODEC = Codecs.validate(RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("namespace").forGetter(target -> target.namespace),
            Path.CODEC.optionalFieldOf("path").forGetter(target -> target.path),
            Codecs.validate(Codec.STRING, regex -> {
                try {
                    Pattern.compile(regex);
                    return DataResult.success(regex);
                } catch (PatternSyntaxException e) {
                    return DataResult.error(() -> "Invalid regex: %s".formatted(e.getMessage()));
                }
            }).optionalFieldOf("regex").forGetter(target -> target.regex)
    ).apply(instance, PatchTarget::new)), target -> {
        if (target.namespace.isEmpty() && target.path.isEmpty() && target.regex.isEmpty())
            return DataResult.error(() -> "Empty targets aren't allowed");
        return DataResult.success(target);
    });

    public static final Codec<PatchTarget> CODEC = Codec.either(ID_CODEC, SPLIT_CODEC)
            .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);

    public static final Codec<List<PatchTarget>> LIST_CODEC = Codec.either(CODEC.listOf(), CODEC).xmap(
            either -> either.map(list -> list, List::of),
            list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));

    @Override
    public boolean test(Identifier identifier) {
        return namespace.map(identifier.getNamespace()::equals).orElse(true)
                && path.map(path -> path.test(identifier.getPath())).orElse(true)
                && regex.map(identifier.toString()::matches).orElse(true);
    }

    public record Path(Either<String, Pair<String, String>> path) implements Predicate<String> {
        public static final Codec<Path> CODEC = Codec.<String, Pair<String, String>>either(Codec.STRING,
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("start").forGetter(Pair::getFirst),
                        Codec.STRING.fieldOf("end").forGetter(Pair::getSecond)
                ).apply(instance, Pair::new))).xmap(Path::new, Path::path);

        @Override
        public boolean test(String path) {
            return this.path.map(path::equals, pair -> path.startsWith(pair.getFirst()) && path.endsWith(pair.getSecond()));
        }
    }
}
