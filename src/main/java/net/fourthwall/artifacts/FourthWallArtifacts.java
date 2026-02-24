package net.fourthwall.artifacts;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fourthwall.artifacts.beacon.BeaconCoreManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fourthwall.artifacts.infested.InfestedArtifactManager;
import net.fourthwall.artifacts.poseidon.PoseidonTridentManager;
import net.fourthwall.artifacts.registry.ModBlocks;
import net.fourthwall.artifacts.registry.ModItemGroups;
import net.fourthwall.artifacts.registry.ModItems;
import net.fourthwall.artifacts.registry.ModStatusEffects;
import net.fourthwall.artifacts.smoldering.SmolderingRodConfig;
import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.fourthwall.artifacts.voidreaver.VoidReaverAuraManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FourthWallArtifacts implements ModInitializer {
    public static final String MOD_ID = "artifacts";
    public static final String CONTENT_NAMESPACE = "evanpack";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} (env={})", MOD_ID, FabricLoader.getInstance().getEnvironmentType());

        try {
            PolymerResourcePackUtils.addModAssets(MOD_ID);
            PolymerResourcePackUtils.markAsRequired();
            LOGGER.info("Polymer resource pack assets registered and marked required for mod '{}'", MOD_ID);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to initialize Polymer resource pack integration. Vanilla-client compatibility may be degraded.", throwable);
        }

        ModStatusEffects.init();
        ModBlocks.init();
        ModItems.init();
        ModItemGroups.init();
        logRegistrySummary("post-registration");

        SmolderingRodConfig config = SmolderingRodConfig.load(LOGGER);
        SmolderingRodManager.init(config);
        VoidReaverAuraManager.init();
        InfestedArtifactManager.init();
        PoseidonTridentManager.init();
        BeaconCoreManager.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Artifacts server-start diagnostics: items={}, blocks={}, groups={}",
                    ModItems.getArtifactItems().size(),
                    countRegisteredArtifactsBlocks(),
                    countRegisteredArtifactsItemGroups());
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
}
