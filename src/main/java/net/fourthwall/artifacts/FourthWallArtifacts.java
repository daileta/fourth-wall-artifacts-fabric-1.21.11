package net.fourthwall.artifacts;

import net.fabricmc.api.ModInitializer;
import net.fourthwall.artifacts.infested.InfestedArtifactManager;
import net.fourthwall.artifacts.registry.ModItemGroups;
import net.fourthwall.artifacts.registry.ModItems;
import net.fourthwall.artifacts.registry.ModStatusEffects;
import net.fourthwall.artifacts.smoldering.SmolderingRodConfig;
import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.fourthwall.artifacts.voidreaver.VoidReaverAuraManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FourthWallArtifacts implements ModInitializer {
    public static final String MOD_ID = "artifacts";
    public static final String CONTENT_NAMESPACE = "evanpack";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModStatusEffects.init();
        ModItems.init();
        ModItemGroups.init();

        SmolderingRodConfig config = SmolderingRodConfig.load(LOGGER);
        SmolderingRodManager.init(config);
        VoidReaverAuraManager.init();
        InfestedArtifactManager.init();
    }

    public static Identifier id(String path) {
        return Identifier.of(CONTENT_NAMESPACE, path);
    }
}
