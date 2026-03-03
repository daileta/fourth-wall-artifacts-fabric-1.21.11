package net.fourthwall.artifacts.excalibur;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.ColorHelper;

public final class ExcaliburManager {

    private ExcaliburManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ExcaliburManager::onEndServerTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ExcaliburManager::onAfterDamage);
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (!ArtifactsConfigManager.get().excalibur.enableParticles) {
            return;
        }

        for (ServerPlayerEntity holder : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingExcalibur(holder)) {
                continue;
            }
            emitExcaliburParticles(holder);
        }
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!isHoldingExcalibur(attacker)) {
            return;
        }
        if (!ArtifactsConfigManager.get().excalibur.enableParticles && !ArtifactsConfigManager.get().excalibur.enableSounds) {
            return;
        }

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        double x = entity.getX();
        double y = entity.getBodyY(0.7);
        double z = entity.getZ();

        if (ArtifactsConfigManager.get().excalibur.enableParticles) {
            // flash particle FIX
            // world.spawnParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

            // warm yellow dust (r=1.0, g=1.0, b=0.6, scale=1.2)
            world.spawnParticles(
                new DustParticleEffect(ColorHelper.fromFloats(1.0f, 1.0f, 1.0f, 0.6f), 1.2f),
                x, y, z, 20, 0.4, 0.2, 0.4, 0.01
            );

            // glow particle
            world.spawnParticles(ParticleTypes.GLOW, x, y, z, 10, 0.4, 0.2, 0.4, 0.0);
        }

        if (ArtifactsConfigManager.get().excalibur.enableSounds) {
            // trident thunder sound, 0.3 volume, 1.2 pitch
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.3F, 1.2F);
        }
    }

    private static boolean isHoldingExcalibur(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.EXCALIBUR) || player.getOffHandStack().isOf(ModItems.EXCALIBUR);
    }

    private static void emitExcaliburParticles(ServerPlayerEntity holder) {
        ServerWorld world = (ServerWorld) holder.getEntityWorld();
        world.spawnParticles(ParticleTypes.GLOW, holder.getX(), holder.getY() + 1.0, holder.getZ(), 4, 0.3, 0.5, 0.3, 0.01);
    }
}
