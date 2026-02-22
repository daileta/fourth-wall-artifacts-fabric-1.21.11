package net.fourthwall.artifacts.smoldering;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SmolderingRodConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "evanpack_smoldering_rod.json";

    public float detonationDamage = 30.0F;
    public int primeFireTicks = 40;
    public int maxPrimedTargetsPerPlayer = 16;
    public boolean friendlyFireRules = false;
    public boolean lastOwnerWins = false;

    public static SmolderingRodConfig load(Logger logger) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        SmolderingRodConfig config = new SmolderingRodConfig();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                SmolderingRodConfig parsed = GSON.fromJson(reader, SmolderingRodConfig.class);
                if (parsed != null) {
                    config = parsed;
                }
            } catch (IOException | JsonParseException e) {
                logger.warn("Failed to read Smoldering Rod config at {}. Using defaults.", path, e);
            }
        }

        config.sanitize();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            logger.warn("Failed to write Smoldering Rod config at {}.", path, e);
        }

        return config;
    }

    private void sanitize() {
        if (detonationDamage < 0.0F) {
            detonationDamage = 0.0F;
        }
        if (primeFireTicks < 0) {
            primeFireTicks = 0;
        }
        if (maxPrimedTargetsPerPlayer < 1) {
            maxPrimedTargetsPerPlayer = 1;
        }
    }
}
