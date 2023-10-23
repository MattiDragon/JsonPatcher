package io.github.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mattidragon.configloader.api.ConfigManager;
import io.github.mattidragon.configloader.api.DefaultedFieldCodec;
import io.github.mattidragon.configloader.api.GenerateMutable;

@GenerateMutable
public record Config(boolean useJavaStacktrace, boolean useShortStacktrace, int patchTimeoutMillis, boolean abortOnFailure) implements MutableConfig.Source {
    private static final Config DEFAULT = new Config(
            false,
            true,
            25,
            true
    );
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultedFieldCodec.of(Codec.BOOL, "use_java_stacktrace", DEFAULT.useJavaStacktrace).forGetter(Config::useJavaStacktrace),
            DefaultedFieldCodec.of(Codec.BOOL, "use_short_stacktrace", DEFAULT.useShortStacktrace).forGetter(Config::useShortStacktrace),
            DefaultedFieldCodec.of(Codec.INT, "patch_timeout_millis", DEFAULT.patchTimeoutMillis).forGetter(Config::patchTimeoutMillis),
            DefaultedFieldCodec.of(Codec.BOOL, "abort_reload_on_failure", DEFAULT.abortOnFailure).forGetter(Config::abortOnFailure)
    ).apply(instance, Config::new));

    public static final ConfigManager<Config> MANAGER = ConfigManager.create(CODEC, DEFAULT, "jsonpatcher");
}
