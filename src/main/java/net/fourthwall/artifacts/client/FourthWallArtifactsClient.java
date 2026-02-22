package net.fourthwall.artifacts.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fourthwall.artifacts.client.render.SmolderingFishingBobberRenderer;
import net.minecraft.entity.EntityType;

public class FourthWallArtifactsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityType.FISHING_BOBBER, SmolderingFishingBobberRenderer::new);
    }
}
