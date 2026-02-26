package net.fourthwall.artifacts.config;

import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArtifactsConfigManager {
    private static final String FILE_NAME = "evanpack_artifacts.json";

    private static volatile ArtifactsConfig config = new ArtifactsConfig();

    private ArtifactsConfigManager() {
    }

    public static ArtifactsConfig load(Logger logger) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        ArtifactsConfig loaded = new ArtifactsConfig();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ArtifactsConfig parsed = ArtifactsConfig.GSON.fromJson(reader, ArtifactsConfig.class);
                if (parsed != null) {
                    loaded = parsed;
                }
            } catch (IOException | JsonParseException exception) {
                logger.warn("Failed to read artifacts config at {}. Using defaults.", path, exception);
            }
        }

        loaded.sanitize();
        write(path, loaded, logger);
        config = loaded;
        return loaded;
    }

    public static ArtifactsConfig get() {
        return config;
    }

    private static void write(Path path, ArtifactsConfig config, Logger logger) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                ArtifactsConfig.GSON.toJson(config.toDocumentedJson(), writer);
            }
        } catch (IOException exception) {
            logger.warn("Failed to write artifacts config at {}.", path, exception);
        }
    }
}
