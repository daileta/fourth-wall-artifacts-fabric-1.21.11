package net.fourthwall.artifacts.earthsplitter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class EarthsplitterManager {

    private EarthsplitterManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(EarthsplitterManager::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        for (ServerPlayerEntity holder : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingEarthsplitter(holder)) {
                continue;
            }

            emitEarthsplitterParticles(holder);
        }
    }

    private static boolean isHoldingEarthsplitter(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.EARTHSPLITTER) || player.getOffHandStack().isOf(ModItems.EARTHSPLITTER);
    }

    private static void emitEarthsplitterParticles(ServerPlayerEntity holder) {
        ServerWorld world = (ServerWorld) holder.getEntityWorld();
        world.spawnParticles(ParticleTypes.CRIT, holder.getX(), holder.getY() + 0.05, holder.getZ(), 6, 0.35, 0.08, 0.35, 0.01);
    }
}