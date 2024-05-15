package io.github.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mattidragon.configloader.api.AlwaysSerializedOptionalFieldCodec;
import io.github.mattidragon.configloader.api.ConfigManager;
import io.github.mattidragon.configloader.api.GenerateMutable;

@GenerateMutable
public record Config(boolean useJavaStacktrace, boolean useShortStacktrace, int patchTimeoutMillis, boolean throwOnFailure, boolean dumpPatchedFiles) implements MutableConfig.Source {
    private static final Config DEFAULT = new Config(
            false,
            true,
            25,
            true,
            false
    );
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "use_java_stacktrace", DEFAULT.useJavaStacktrace).forGetter(Config::useJavaStacktrace),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "use_short_stacktrace", DEFAULT.useShortStacktrace).forGetter(Config::useShortStacktrace),
            AlwaysSerializedOptionalFieldCodec.create(Codec.INT, "patch_timeout_millis", DEFAULT.patchTimeoutMillis).forGetter(Config::patchTimeoutMillis),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "throw_on_failure", DEFAULT.throwOnFailure).forGetter(Config::throwOnFailure),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "dump_patched_files", DEFAULT.dumpPatchedFiles).forGetter(Config::dumpPatchedFiles)
    ).apply(instance, Config::new));

    public static final ConfigManager<Config> MANAGER = ConfigManager.create(CODEC, DEFAULT, "jsonpatcher");
}