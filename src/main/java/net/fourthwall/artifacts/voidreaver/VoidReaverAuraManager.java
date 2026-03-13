package net.fourthwall.artifacts.voidreaver;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.integration.EmptyEmbraceArtifactSuppression;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
        ServerLivingEntityEvents.AFTER_DAMAGE.register(VoidReaverAuraManager::onAfterDamage);
    }

    private static void onEndServerTick(MinecraftServer server) {
        for (ServerPlayerEntity holder : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingVoidReaver(holder)) {
                continue;
            }

            if (ArtifactsConfigManager.get().voidReaver.enableParticles) {
                emitVoidReaverParticles(holder);
            }
            erasePositiveEffectsAround(holder);
        }
    }

    private static void onAfterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || !ArtifactsConfigManager.get().voidReaver.enableParticles) {
            return;
        }
        var weapon = source.getWeaponStack();
        if (weapon == null || !weapon.isOf(ModItems.VOID_REAVER) || !(target.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        double x = target.getX();
        double y = target.getBodyY(0.55D);
        double z = target.getZ();
        world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 18, 0.4D, 0.3D, 0.4D, 0.08D);
        world.spawnParticles(ParticleTypes.TRIAL_OMEN, x, y, z, 14, 0.4D, 0.3D, 0.4D, 0.01D);
    }

    private static boolean isHoldingVoidReaver(PlayerEntity player) {
        if (EmptyEmbraceArtifactSuppression.areArtifactPowersSuppressed(player)) {
            return false;
        }
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
