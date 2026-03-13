package net.fourthwall.artifacts.particle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.integration.EmptyEmbraceArtifactSuppression;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class WearerParticleManager {
    private static final long PARTICLE_INTERVAL_TICKS = 8L;

    private WearerParticleManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(WearerParticleManager::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (server.getTicks() % PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            double x = player.getX();
            double y = player.getBodyY(0.6D);
            double z = player.getZ();
            boolean armorSuppressed = EmptyEmbraceArtifactSuppression.isArtifactArmorSuppressed(player);

            if (!armorSuppressed && ArtifactsConfigManager.get().emperorsCrown.enableParticles && player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.EMPERORS_CROWN)) {
                world.spawnParticles(ParticleTypes.GLOW, x, y, z, 6, 3.0D, 0.9D, 3.0D, 0.01D);
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 4, 3.0D, 0.9D, 3.0D, 0.005D);
            }

            if (!armorSuppressed && ArtifactsConfigManager.get().lionsHeartChestplate.enableParticles && player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.LIONS_HEART)) {
                world.spawnParticles(ParticleTypes.HEART, x, y, z, 4, 0.65D, 0.35D, 0.65D, 0.0D);
                world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 4, 0.65D, 0.35D, 0.65D, 0.01D);
            }
        }
    }
}
