package io.github.mattidragon.jsonpatch.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mattidragon.configloader.api.ConfigManager;
import io.github.mattidragon.configloader.api.DefaultedFieldCodec;
import io.github.mattidragon.configloader.api.GenerateMutable;

@GenerateMutable
public record Config(boolean useJavaStacktrace) implements MutableConfig.Source {
    private static final Config DEFAULT = new Config(false);
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultedFieldCodec.of(Codec.BOOL, "use_java_stacktrace", DEFAULT.useJavaStacktrace).forGetter(Config::useJavaStacktrace)
    ).apply(instance, Config::new));

    public static final ConfigManager<Config> MANAGER = ConfigManager.create(CODEC, DEFAULT, "jsonpatch");
}
