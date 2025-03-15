package dev.mattidragon.jsonpatcher;

import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.misc.DumpManager;
import dev.mattidragon.jsonpatcher.patch.ErrorLogger;
import dev.mattidragon.jsonpatcher.patch.global.GlobalDirSetup;
import dev.mattidragon.jsonpatcher.patch.global.GlobalPatch;
import dev.mattidragon.jsonpatcher.patch.global.GlobalPatchLoader;
import dev.mattidragon.jsonpatcher.remap.MappingsLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class JsonPatcher implements ModInitializer {
    public static final Path DATA_DIR = FabricLoader.getInstance().getGameDir().resolve("jsonpatcher");
    private static final Set<String> SUPPORTED_VERSIONS = new HashSet<>(Set.of("2"));
    public static final String MOD_ID = "jsonpatcher";
    public static final Logger MAIN_LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String RELOAD_LOGGER_NAME = "JsonPatcher Reload";
    public static final Logger RELOAD_LOGGER = LoggerFactory.getLogger(RELOAD_LOGGER_NAME);

    static {
        hackLog4j();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        MappingsLoader.init();
        Config.MANAGER.get();
        DumpManager.cleanDump("");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> ErrorLogger.CURRENT.set(error -> {
            var manager = server.getPlayerManager();
            for (var player : manager.getPlayerList()) {
                if (manager.isOperator(player.getGameProfile())) {
                    player.sendMessage(error);
                }
            }
        }));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ErrorLogger.CURRENT.remove());

        GlobalDirSetup.setupDirs();
        GlobalPatchLoader.loadGlobalPatches();
        GlobalPatchLoader.runEntrypoint(GlobalPatch.Entrypoint.MAIN);
    }

    /**
     * Uses log4j core apis to reconfigure logging of patches into a custom file.
     * Should be stable enough, but just in case we handle errors gracefully.
     */
    private static void hackLog4j() {
        MAIN_LOGGER.debug("About to hack log4j config");
        try {
            var log4jLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(RELOAD_LOGGER_NAME);
            var configuration = log4jLogger.getContext().getConfiguration();

            var appender = RandomAccessFileAppender.newBuilder()
                    .setName("JsonPatcherFile")
                    .setFileName("jsonpatcher/jsonpatcher.log")
                    .setLayout(PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level] (%logger) %msg{nolookups}%n").build())
                    .setAppend(false)
                    .build();
            appender.start();

            configuration.addAppender(appender);
            configuration.addLoggerAppender(log4jLogger, appender);
            configuration.setLoggerAdditive(log4jLogger, false);

            log4jLogger.setLevel(Level.toLevel(System.getProperty("jsonpatcher.log.level"), Level.INFO));

            MAIN_LOGGER.debug("Successfully hacked log4j config. Now we have our own file!");
        } catch (IncompatibleClassChangeError | NoClassDefFoundError | RuntimeException e) {
            MAIN_LOGGER.error("Failed to hack log4j. All output will be logged to main log.", e);
        }
    }

    public static boolean isSupportedVersion(String version) {
        return SUPPORTED_VERSIONS.contains(version);
    }
}
