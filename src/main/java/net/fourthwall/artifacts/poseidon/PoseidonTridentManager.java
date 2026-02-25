package net.fourthwall.artifacts.poseidon;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PoseidonTridentManager {
    private static final int HOLDING_BUFF_DURATION_TICKS = 10;
    private static final int SPECIAL_COOLDOWN_TICKS = 300;
    private static final int MAX_CHANNELING_TICKS = 100;
    private static final int BEAM_RADIUS = 6;
    private static final int BEAM_VISUAL_INTERVAL = 2;
    private static final int BEAM_DAMAGE_INTERVAL = 10;
    private static final float BEAM_DAMAGE = 6.0F;
    private static final float RETALIATION_DAMAGE = 8.0F;
    private static final Map<UUID, Integer> SPECIAL_COOLDOWNS = new HashMap<>();
    private static final Set<UUID> CHANNELING_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHANNELING_DURATIONS = new HashMap<>();
    private static final Map<UUID, Vec3d> CHANNELING_LOCK_POSITIONS = new HashMap<>();
    private static int retaliationDepth = 0;

    private PoseidonTridentManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PoseidonTridentManager::onEndServerTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PoseidonTridentManager::onAfterDamage);
    }

    private static void onEndServerTick(MinecraftServer server) {
        long tick = server.getTicks();
        tickCooldowns();
        Set<UUID> channelingThisTick = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingPoseidonTrident(player)) {
                continue;
            }

            applyHoldingBuffs(player);

            if (!canUsePoseidonSpecial(player)) {
                continue;
            }

            UUID uuid = player.getUuid();
            int nextChannelTicks = CHANNELING_DURATIONS.getOrDefault(uuid, 0) + 1;
            if (nextChannelTicks > MAX_CHANNELING_TICKS) {
                SPECIAL_COOLDOWNS.put(uuid, SPECIAL_COOLDOWN_TICKS);
                CHANNELING_DURATIONS.remove(uuid);
                CHANNELING_LOCK_POSITIONS.remove(uuid);
                continue;
            }

            CHANNELING_DURATIONS.put(uuid, nextChannelTicks);
            channelingThisTick.add(uuid);
            CHANNELING_LOCK_POSITIONS.putIfAbsent(uuid, new Vec3d(player.getX(), player.getY(), player.getZ()));
            lockPlayerMovement(player);
            spawnStanceParticles(player, tick);

            List<LivingEntity> targets = getBeamTargets(player);
            if (targets.isEmpty()) {
                continue;
            }

            if (tick % BEAM_VISUAL_INTERVAL == 0) {
                spawnBeamVisuals(player, targets);
            }

            if (tick % BEAM_DAMAGE_INTERVAL == 0) {
                fireBeams(player, targets);
            }
        }

        for (UUID uuid : Set.copyOf(CHANNELING_PLAYERS)) {
            if (!channelingThisTick.contains(uuid)) {
                SPECIAL_COOLDOWNS.put(uuid, SPECIAL_COOLDOWN_TICKS);
                CHANNELING_DURATIONS.remove(uuid);
                CHANNELING_LOCK_POSITIONS.remove(uuid);
            }
        }

        CHANNELING_PLAYERS.clear();
        CHANNELING_PLAYERS.addAll(channelingThisTick);
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (retaliationDepth > 0 || blocked || damageTaken <= 0.0F) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!canUsePoseidonSpecial(player)) {
            return;
        }
        if (!(source.getAttacker() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker == player) {
            return;
        }

        retaliationDepth++;
        try {
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            attacker.damage(world, attacker.getDamageSources().playerAttack(player), RETALIATION_DAMAGE);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(), 10, 0.25, 0.25, 0.25, 0.02);
            world.spawnParticles(ParticleTypes.SPLASH, attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(), 8, 0.25, 0.2, 0.25, 0.03);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.8F, 1.25F);
        } finally {
            retaliationDepth--;
        }
    }

    private static void applyHoldingBuffs(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, HOLDING_BUFF_DURATION_TICKS, 0, true, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, HOLDING_BUFF_DURATION_TICKS + 40, 0, true, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, HOLDING_BUFF_DURATION_TICKS, 0, true, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, HOLDING_BUFF_DURATION_TICKS, 0, true, false));
    }

    private static List<LivingEntity> getBeamTargets(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        return world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(BEAM_RADIUS),
                target -> isValidBeamTarget(player, target)
        );
    }

    private static boolean isValidBeamTarget(ServerPlayerEntity player, LivingEntity target) {
        if (!target.isAlive() || target == player) {
            return false;
        }
        if (target instanceof PlayerEntity otherPlayer && (otherPlayer.isSpectator() || otherPlayer.isCreative())) {
            return false;
        }
        return hasLineOfSight(player, target);
    }

    private static void fireBeams(ServerPlayerEntity player, List<LivingEntity> targets) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.7F, 1.0F);

        for (LivingEntity target : targets) {
            target.damage(world, target.getDamageSources().playerAttack(player), BEAM_DAMAGE);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.BUBBLE, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.25, 0.25, 0.25, 0.02);
        }
    }

    private static void spawnBeamVisuals(ServerPlayerEntity player, List<LivingEntity> targets) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d origin = player.getEyePos();

        for (LivingEntity target : targets) {
            Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.55), target.getZ());
            Vec3d delta = end.subtract(origin);
            int steps = Math.max(8, (int) (delta.length() * 4.0));
            Vec3d step = delta.multiply(1.0 / steps);

            for (int i = 0; i <= steps; i++) {
                Vec3d point = origin.add(step.multiply(i));
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    world.spawnParticles(ParticleTypes.NAUTILUS, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
    }

    private static void spawnStanceParticles(ServerPlayerEntity player, long tick) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        double x = player.getX();
        double y = player.getY() + 0.9;
        double z = player.getZ();

        world.spawnParticles(ParticleTypes.NAUTILUS, x, y, z, 12, 0.6, 0.7, 0.6, 0.01);
        world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 10, 0.5, 0.6, 0.5, 0.02);
        if (tick % 4 == 0) {
            world.spawnParticles(ParticleTypes.SPLASH, x, player.getY() + 0.1, z, 8, 0.45, 0.1, 0.45, 0.03);
        }
    }

    private static boolean hasLineOfSight(ServerPlayerEntity player, LivingEntity target) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d start = player.getEyePos();
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.55), target.getZ());

        HitResult hitResult = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return hitResult.getType() == HitResult.Type.MISS;
    }

    private static void lockPlayerMovement(ServerPlayerEntity player) {
        Vec3d lockPos = CHANNELING_LOCK_POSITIONS.get(player.getUuid());
        if (lockPos == null) {
            return;
        }

        player.setVelocity(0.0, 0.0, 0.0);
        if (player.squaredDistanceTo(lockPos.x, lockPos.y, lockPos.z) > 0.0001D) {
            player.refreshPositionAfterTeleport(lockPos.x, lockPos.y, lockPos.z);
        }
    }

    private static boolean isHoldingPoseidonTrident(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.TRIDENT_OF_POSEIDON) || player.getOffHandStack().isOf(ModItems.TRIDENT_OF_POSEIDON);
    }

    private static boolean canUsePoseidonSpecial(ServerPlayerEntity player) {
        return isPoseidonStanceConditionsMet(player) && !isHoldingShield(player) && !isOnCooldown(player);
    }

    private static boolean isPoseidonStanceConditionsMet(ServerPlayerEntity player) {
        return isHoldingPoseidonTrident(player) && player.isSneaking() && player.isTouchingWaterOrRain();
    }

    private static boolean isHoldingShield(PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.SHIELD) || player.getOffHandStack().isOf(Items.SHIELD);
    }

    private static boolean isOnCooldown(PlayerEntity player) {
        return SPECIAL_COOLDOWNS.getOrDefault(player.getUuid(), 0) > 0;
    }

    private static void tickCooldowns() {
        for (UUID uuid : List.copyOf(SPECIAL_COOLDOWNS.keySet())) {
            int next = SPECIAL_COOLDOWNS.get(uuid) - 1;
            if (next <= 0) {
                SPECIAL_COOLDOWNS.remove(uuid);
            } else {
                SPECIAL_COOLDOWNS.put(uuid, next);
            }
        }
    }
}
