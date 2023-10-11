package io.github.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mattidragon.configloader.api.ConfigManager;
import io.github.mattidragon.configloader.api.DefaultedFieldCodec;
import io.github.mattidragon.configloader.api.GenerateMutable;

@GenerateMutable
public record Config(boolean useJavaStacktrace, boolean useShortStacktrace) implements MutableConfig.Source {
    private static final Config DEFAULT = new Config(false, true);
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultedFieldCodec.of(Codec.BOOL, "use_java_stacktrace", DEFAULT.useJavaStacktrace).forGetter(Config::useJavaStacktrace),
            DefaultedFieldCodec.of(Codec.BOOL, "use_short_stacktrace", DEFAULT.useShortStacktrace).forGetter(Config::useShortStacktrace)
    ).apply(instance, Config::new));

    public static final ConfigManager<Config> MANAGER = ConfigManager.create(CODEC, DEFAULT, "jsonpatcher");
}
