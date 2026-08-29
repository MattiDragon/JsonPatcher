package dev.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import io.github.mattidragon.configloader.api.AlwaysSerializedOptionalFieldCodec;
import io.github.mattidragon.configloader.api.ConfigManager;
import io.github.mattidragon.configloader.api.GenerateMutable;

import java.util.Map;

@GenerateMutable
public record Config(
        int patchTimeoutMillis,
        boolean throwOnFailure,
        boolean dumpPatchedFiles,
        boolean dumpCompiledPatches,
        TrustLevel reflectionMinTrustLevel,
        boolean hotswapEvents,
        FeatureFlagMap featureFlags
) implements MutableConfig.Source {
    private static final Config DEFAULT = new Config(
            500,
            true,
            false,
            false,
            TrustLevel.MODPACK,
            true,
            new FeatureFlagMap(Map.of())
    );
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AlwaysSerializedOptionalFieldCodec.create(Codec.INT, "patch_timeout_millis", DEFAULT.patchTimeoutMillis).forGetter(Config::patchTimeoutMillis),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "throw_on_failure", DEFAULT.throwOnFailure).forGetter(Config::throwOnFailure),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "dump_patched_files", DEFAULT.dumpPatchedFiles).forGetter(Config::dumpPatchedFiles),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "dump_compiled_patches", DEFAULT.dumpCompiledPatches).forGetter(Config::dumpCompiledPatches),
            AlwaysSerializedOptionalFieldCodec.create(TrustLevel.CODEC, "reflection_min_trust_level", DEFAULT.reflectionMinTrustLevel).forGetter(Config::reflectionMinTrustLevel),
            AlwaysSerializedOptionalFieldCodec.create(Codec.BOOL, "hotswap_events", DEFAULT.hotswapEvents).forGetter(Config::hotswapEvents),
            AlwaysSerializedOptionalFieldCodec.create(FeatureFlagMap.CODEC, "feature_flags", DEFAULT.featureFlags).forGetter(Config::featureFlags)
    ).apply(instance, Config::new));

    public static final ConfigManager<Config> MANAGER = ConfigManager.create(CODEC, DEFAULT, "jsonpatcher");
    }