package net.fourthwall.artifacts.voidreaver;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public final class VoidReaverAuraManager {
    private static final double EFFECT_RADIUS = 10.0D;

    private VoidReaverAuraManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(VoidReaverAuraManager::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        for (ServerPlayerEntity holder : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingVoidReaver(holder)) {
                continue;
            }

            emitVoidReaverParticles(holder);
            erasePositiveEffectsAround(holder);
        }
    }

    private static boolean isHoldingVoidReaver(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.VOID_REAVER) || player.getOffHandStack().isOf(ModItems.VOID_REAVER);
    }

    private static boolean isHoldingVoidReaver(LivingEntity entity) {
        return entity instanceof PlayerEntity player && isHoldingVoidReaver(player);
    }

    private static void emitVoidReaverParticles(ServerPlayerEntity holder) {
        ServerWorld world = (ServerWorld) holder.getEntityWorld();
        world.spawnParticles(DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F), holder.getX(), holder.getY() + 0.05, holder.getZ(), 16, 0.35, 0.08, 0.35, 0.01);
    }

    private static void erasePositiveEffectsAround(ServerPlayerEntity holder) {
        ServerWorld world = (ServerWorld) holder.getEntityWorld();
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                holder.getBoundingBox().expand(EFFECT_RADIUS),
                entity -> entity.isAlive() && entity != holder && !isHoldingVoidReaver(entity)
        );

        for (LivingEntity target : targets) {
            List<StatusEffectInstance> effects = new ArrayList<>(target.getStatusEffects());
            for (StatusEffectInstance instance : effects) {
                if (instance.getEffectType().value().isBeneficial()) {
                    target.removeStatusEffect(instance.getEffectType());
                }
            }
        }
    }
}
