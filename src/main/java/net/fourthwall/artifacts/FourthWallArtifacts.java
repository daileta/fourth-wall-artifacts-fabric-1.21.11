package net.fourthwall.artifacts;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fourthwall.artifacts.beacon.BeaconCoreManager;
import net.fourthwall.artifacts.blood.BloodSacrificeManager;
import net.fourthwall.artifacts.command.ArtifactsCommands;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fourthwall.artifacts.earthsplitter.EarthsplitterManager;
import net.fourthwall.artifacts.excalibur.ExcaliburManager;
import net.fourthwall.artifacts.infested.InfestedArtifactManager;
import net.fourthwall.artifacts.particle.WearerParticleManager;
import net.fourthwall.artifacts.poseidon.PoseidonTridentManager;
import net.fourthwall.artifacts.repeater.RepeaterManager;
import net.fourthwall.artifacts.registry.ModBlocks;
import net.fourthwall.artifacts.registry.ModItemGroups;
import net.fourthwall.artifacts.registry.ModItems;
import net.fourthwall.artifacts.registry.ModStatusEffects;
import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.fourthwall.artifacts.undead.UndeadWardArmyManager;
import net.fourthwall.artifacts.voidreaver.VoidReaverAuraManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FourthWallArtifacts implements ModInitializer {
    public static final String MOD_ID = "artifacts";
    public static final String CONTENT_NAMESPACE = "evanpack";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} (env={})", MOD_ID, FabricLoader.getInstance().getEnvironmentType());

        try {
            PolymerResourcePackUtils.addModAssets(MOD_ID);
            LOGGER.info("Polymer resource pack assets registered for mod '{}'", MOD_ID);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to initialize Polymer resource pack integration. Vanilla-client compatibility may be degraded.", throwable);
        }
        logPolymerAutoHostDiagnostics();

        ArtifactsConfigManager.load(LOGGER);
        ArtifactsCommands.init();
        ModStatusEffects.init();
        ModBlocks.init();
        ModItems.init();
        ModItemGroups.init();
        logRegistrySummary("post-registration");

        SmolderingRodManager.init();
        BloodSacrificeManager.init();
        VoidReaverAuraManager.init();
        InfestedArtifactManager.init();
        PoseidonTridentManager.init();
        RepeaterManager.init();
        BeaconCoreManager.init();
        EarthsplitterManager.init();
        ExcaliburManager.init();
        UndeadWardArmyManager.init();
        WearerParticleManager.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Artifacts server-start diagnostics: items={}, blocks={}, groups={}",
                    ModItems.getArtifactItems().size(),
                    countRegisteredArtifactsBlocks(),
                    countRegisteredArtifactsItemGroups());
            logPolymerPackFileDiagnostics();
            LOGGER.info("Vanilla clients will not have a custom creative tab. Use server commands/mechanics to obtain Polymer-backed items.");
            logRegistrySummary("server-started");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> LOGGER.info(
                "Player joined '{}'; artifacts registry check: first item '{}' present={}",
                handler.player.getName().getString(),
                ModItems.getArtifactItems().isEmpty() ? "<none>" : Registries.ITEM.getId(ModItems.getArtifactItems().getFirst()),
                ModItems.getArtifactItems().isEmpty() || Registries.ITEM.containsId(Registries.ITEM.getId(ModItems.getArtifactItems().getFirst()))
        ));
    }

    public static Identifier id(String path) {
        return Identifier.of(CONTENT_NAMESPACE, path);
    }

    private static void logRegistrySummary(String stage) {
        LOGGER.info("Artifacts registry summary ({}): items={}, blocks={}, itemGroups={}",
                stage,
                ModItems.describeRegistrations(),
                ModBlocks.describeRegistrations(),
                ModItemGroups.describeRegistrations());
    }

    private static long countRegisteredArtifactsBlocks() {
        return Registries.BLOCK.getIds().stream().filter(id -> CONTENT_NAMESPACE.equals(id.getNamespace())).count();
    }

    private static long countRegisteredArtifactsItemGroups() {
        return Registries.ITEM_GROUP.getIds().stream().filter(id -> CONTENT_NAMESPACE.equals(id.getNamespace())).count();
    }

    private static void logPolymerAutoHostDiagnostics() {
        if (FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.SERVER) {
            return;
        }

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("polymer/auto-host.json");
        if (!Files.exists(configPath)) {
            LOGGER.warn("Polymer AutoHost config not found at {}. Vanilla clients will only see fallback item textures/models unless you host and send a resource pack another way.", configPath);
            return;
        }

        try {
            String content = Files.readString(configPath);
            boolean enabled = content.contains("\"enabled\"") && content.contains("true");
            if (enabled) {
                LOGGER.info("Polymer AutoHost config detected at {} (enabled=true). Ensure the advertised address is reachable by players.", configPath);
            } else {
                LOGGER.warn("Polymer AutoHost config exists at {} but may not be enabled. Set \"enabled\": true so vanilla clients receive custom item models/textures.", configPath);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not read Polymer AutoHost config at {}: {}", configPath, exception.getMessage());
        }
    }

    private static void logPolymerPackFileDiagnostics() {
        Path packPath = PolymerResourcePackUtils.getMainPath();
        boolean exists = Files.exists(packPath);
        try {
            long size = exists ? Files.size(packPath) : -1L;
            LOGGER.info("Polymer pack file diagnostics: path='{}', exists={}, sizeBytes={}", packPath, exists, size);
        } catch (IOException exception) {
            LOGGER.warn("Could not inspect Polymer pack file at {}: {}", packPath, exception.getMessage());
        }
    }
}
