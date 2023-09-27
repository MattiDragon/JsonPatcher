package io.github.mattidragon.jsonpatcher;

import io.github.mattidragon.jsonpatcher.config.Config;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonPatcher implements ModInitializer {
    public static final String MOD_ID = "jsonpatcher";
    public static final Logger MAIN_LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String RELOAD_LOGGER_NAME = "JsonPatcher Reload";
    public static final Logger RELOAD_LOGGER = LoggerFactory.getLogger(RELOAD_LOGGER_NAME);

    static {
        hackLog4j();
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Config.MANAGER.get();
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
                    .setFileName("logs/jsonpatcher.log")
                    .setLayout(PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level] (%logger) %msg{nolookups}%n").build())
                    .setAppend(false)
                    .build();
            appender.start();

            configuration.addAppender(appender);
            configuration.addLoggerAppender(log4jLogger, appender);
            configuration.setLoggerAdditive(log4jLogger, false);

            MAIN_LOGGER.debug("Successfully hacked log4j config. Now we have our own file!");
        } catch (IncompatibleClassChangeError | NoClassDefFoundError | RuntimeException e) {
            MAIN_LOGGER.error("Failed to hack log4j. All output will be logged to main log.", e);
        }
    }

}
