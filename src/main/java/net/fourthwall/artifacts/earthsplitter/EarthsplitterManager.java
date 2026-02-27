package net.fourthwall.artifacts.earthsplitter;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class EarthsplitterManager {
    private static final int HELD_PARTICLE_INTERVAL_TICKS = 4;
    private static final int SMASH_BURST_DURATION_TICKS = 20;
    private static final Map<UUID, SmashBurstState> ACTIVE_SMASH_BURSTS = new HashMap<>();

    private EarthsplitterManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(EarthsplitterManager::onEndServerTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(EarthsplitterManager::onAfterDamage);
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (!ArtifactsConfigManager.get().earthsplitter.enableParticles) {
            ACTIVE_SMASH_BURSTS.clear();
            return;
        }

        if (server.getTicks() % HELD_PARTICLE_INTERVAL_TICKS == 0L) {
            for (ServerPlayerEntity holder : server.getPlayerManager().getPlayerList()) {
                if (!isHoldingEarthsplitter(holder)) {
                    continue;
                }

                emitEarthsplitterParticles(holder);
            }
        }

        tickSmashBursts(server);
    }

    private static void onAfterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || !ArtifactsConfigManager.get().earthsplitter.enableParticles) {
            return;
        }

        ItemStack weapon = source.getWeaponStack();
        if (weapon == null || !weapon.isOf(ModItems.EARTHSPLITTER)) {
            return;
        }
        if (!(target.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        if (source.isOf(DamageTypes.MACE_SMASH)) {
            ACTIVE_SMASH_BURSTS.put(target.getUuid(), new SmashBurstState(world.getRegistryKey(), SMASH_BURST_DURATION_TICKS));
            return;
        }

        world.spawnParticles(ParticleTypes.SCRAPE, target.getX(), target.getBodyY(0.55D), target.getZ(), 20, 0.45D, 0.28D, 0.45D, 0.01D);
    }

    private static void tickSmashBursts(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SmashBurstState>> iterator = ACTIVE_SMASH_BURSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SmashBurstState> entry = iterator.next();
            SmashBurstState burst = entry.getValue();
            ServerWorld world = server.getWorld(burst.worldKey());
            if (world == null) {
                iterator.remove();
                continue;
            }

            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            double x = target.getX();
            double y = target.getBodyY(0.5D);
            double z = target.getZ();
            world.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 2, 0.35D, 0.2D, 0.35D, 0.01D);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 4, 0.35D, 0.2D, 0.35D, 0.0D);

            burst.decrement();
            if (burst.ticksRemaining() <= 0) {
                world.spawnParticles(TintedParticleEffect.create(ParticleTypes.FLASH, 1.0F, 1.0F, 1.0F), x, y, z, 26, 0.6D, 0.45D, 0.6D, 0.0D);
                iterator.remove();
            }
        }
    }

    private static boolean isHoldingEarthsplitter(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.EARTHSPLITTER) || player.getOffHandStack().isOf(ModItems.EARTHSPLITTER);
    }

    private static void emitEarthsplitterParticles(ServerPlayerEntity holder) {
        ServerWorld world = (ServerWorld) holder.getEntityWorld();
        world.spawnParticles(ParticleTypes.CRIT, holder.getX(), holder.getY() + 0.05, holder.getZ(), 6, 0.35, 0.08, 0.35, 0.01);
    }

    private static final class SmashBurstState {
        private final RegistryKey<World> worldKey;
        private int ticksRemaining;

        private SmashBurstState(RegistryKey<World> worldKey, int ticksRemaining) {
            this.worldKey = worldKey;
            this.ticksRemaining = ticksRemaining;
        }

        private RegistryKey<World> worldKey() {
            return worldKey;
        }

        private int ticksRemaining() {
            return ticksRemaining;
        }

        private void decrement() {
            ticksRemaining--;
        }
    }
}
