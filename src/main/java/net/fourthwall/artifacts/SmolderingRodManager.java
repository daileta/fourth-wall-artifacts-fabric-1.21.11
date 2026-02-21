package net.fourthwall.artifacts;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SmolderingRodManager {
    private static final Map<UUID, Set<UUID>> PRIMED_BY_OWNER = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNERS_BY_TARGET = new HashMap<>();
    private static final Map<UUID, UUID> ACTIVE_BOBBERS = new HashMap<>();
    private static SmolderingRodConfig config;

    private SmolderingRodManager() {
    }

    public static void init(SmolderingRodConfig cfg) {
        config = cfg;

        ServerTickEvents.END_SERVER_TICK.register(SmolderingRodManager::onEndServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearOwner(handler.player.getUuid()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> removeTarget(entity.getUuid()));
    }

    public static void onBobberCast(PlayerEntity owner, FishingBobberEntity bobber) {
        ACTIVE_BOBBERS.put(owner.getUuid(), bobber.getUuid());
    }

    public static void onBobberRemoved(PlayerEntity owner, net.minecraft.world.World world) {
        UUID ownerId = owner.getUuid();
        ACTIVE_BOBBERS.remove(ownerId);
        if (world instanceof ServerWorld serverWorld) {
            detonateForOwner(ownerId, serverWorld.getServer());
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (config == null) {
            return;
        }

        List<UUID> ownersToDetonate = new ArrayList<>();

        for (Map.Entry<UUID, UUID> entry : ACTIVE_BOBBERS.entrySet()) {
            UUID ownerId = entry.getKey();
            UUID bobberId = entry.getValue();

            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
            if (owner == null || !owner.isAlive()) {
                ownersToDetonate.add(ownerId);
                continue;
            }

            Entity bobberEntity = findEntity(server, bobberId);
            if (!(bobberEntity instanceof FishingBobberEntity bobber) || !bobber.isAlive()) {
                ownersToDetonate.add(ownerId);
                continue;
            }

            cleanupDeadTargets(server, ownerId);
            primeNearestTarget(owner, bobber);
        }

        for (UUID ownerId : ownersToDetonate) {
            ACTIVE_BOBBERS.remove(ownerId);
            detonateForOwner(ownerId, server);
        }
    }

    private static void primeNearestTarget(ServerPlayerEntity owner, FishingBobberEntity bobber) {
        ServerWorld world = (ServerWorld) bobber.getEntityWorld();
        Box searchBox = bobber.getBoundingBox().expand(config.primeRadius);
        Set<UUID> ownerPrimed = PRIMED_BY_OWNER.computeIfAbsent(owner.getUuid(), ignored -> new HashSet<>());

        LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;

        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> isValidPrimeTarget(owner, entity))) {
            double sq = candidate.squaredDistanceTo(bobber);
            if (sq <= config.primeRadius * config.primeRadius && sq < nearestSq) {
                nearestSq = sq;
                nearest = candidate;
            }
        }

        if (nearest == null) {
            return;
        }

        UUID targetId = nearest.getUuid();
        if (ownerPrimed.contains(targetId)) {
            return;
        }
        if (ownerPrimed.size() >= config.maxPrimedTargetsPerPlayer) {
            return;
        }

        if (config.lastOwnerWins) {
            Set<UUID> existingOwners = OWNERS_BY_TARGET.get(targetId);
            if (existingOwners != null) {
                for (UUID existingOwner : List.copyOf(existingOwners)) {
                    removePrime(existingOwner, targetId);
                }
            }
        }

        ownerPrimed.add(targetId);
        OWNERS_BY_TARGET.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(owner.getUuid());
        nearest.setFireTicks(Math.max(nearest.getFireTicks(), config.primeFireTicks));

        world.spawnParticles(ParticleTypes.SMOKE, nearest.getX(), nearest.getBodyY(0.5), nearest.getZ(), 8, 0.25, 0.25, 0.25, 0.01);
        world.spawnParticles(ParticleTypes.FLAME, nearest.getX(), nearest.getBodyY(0.5), nearest.getZ(), 6, 0.2, 0.25, 0.2, 0.005);
    }

    private static boolean isValidPrimeTarget(ServerPlayerEntity owner, LivingEntity candidate) {
        if (candidate == owner) {
            return false;
        }
        if (candidate instanceof ArmorStandEntity) {
            return false;
        }
        if (!candidate.isAlive()) {
            return false;
        }
        if (!config.friendlyFireRules && candidate instanceof PlayerEntity playerTarget && owner.isTeammate(playerTarget)) {
            return false;
        }
        return true;
    }

    private static void detonateForOwner(UUID ownerId, MinecraftServer server) {
        Set<UUID> primed = PRIMED_BY_OWNER.remove(ownerId);
        if (primed == null || primed.isEmpty()) {
            return;
        }

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
        for (UUID targetId : List.copyOf(primed)) {
            Entity entity = findEntity(server, targetId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                removePrime(ownerId, targetId);
                continue;
            }

            ServerWorld world = (ServerWorld) target.getEntityWorld();
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F, world.getRandom().nextLong());
            world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getBodyY(0.5), target.getZ(), 2, 0.2, 0.2, 0.2, 0.01);

            DamageSource source = owner != null
                    ? target.getDamageSources().explosion(owner, owner)
                    : target.getDamageSources().explosion(target, null);
            target.damage(world, source, config.detonationDamage);
            removePrime(ownerId, targetId);
        }
    }

    private static void cleanupDeadTargets(MinecraftServer server, UUID ownerId) {
        Set<UUID> primed = PRIMED_BY_OWNER.get(ownerId);
        if (primed == null || primed.isEmpty()) {
            return;
        }

        Iterator<UUID> iterator = primed.iterator();
        while (iterator.hasNext()) {
            UUID targetId = iterator.next();
            Entity entity = findEntity(server, targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                iterator.remove();
                Set<UUID> owners = OWNERS_BY_TARGET.get(targetId);
                if (owners != null) {
                    owners.remove(ownerId);
                    if (owners.isEmpty()) {
                        OWNERS_BY_TARGET.remove(targetId);
                    }
                }
            }
        }

        if (primed.isEmpty()) {
            PRIMED_BY_OWNER.remove(ownerId);
        }
    }

    private static void clearOwner(UUID ownerId) {
        ACTIVE_BOBBERS.remove(ownerId);
        Set<UUID> primed = PRIMED_BY_OWNER.remove(ownerId);
        if (primed == null) {
            return;
        }
        for (UUID targetId : primed) {
            Set<UUID> owners = OWNERS_BY_TARGET.get(targetId);
            if (owners != null) {
                owners.remove(ownerId);
                if (owners.isEmpty()) {
                    OWNERS_BY_TARGET.remove(targetId);
                }
            }
        }
    }

    private static void removeTarget(UUID targetId) {
        Set<UUID> owners = OWNERS_BY_TARGET.remove(targetId);
        if (owners == null) {
            return;
        }
        for (UUID ownerId : owners) {
            Set<UUID> ownerPrimed = PRIMED_BY_OWNER.get(ownerId);
            if (ownerPrimed != null) {
                ownerPrimed.remove(targetId);
                if (ownerPrimed.isEmpty()) {
                    PRIMED_BY_OWNER.remove(ownerId);
                }
            }
        }
    }

    private static void removePrime(UUID ownerId, UUID targetId) {
        Set<UUID> ownerSet = PRIMED_BY_OWNER.get(ownerId);
        if (ownerSet != null) {
            ownerSet.remove(targetId);
            if (ownerSet.isEmpty()) {
                PRIMED_BY_OWNER.remove(ownerId);
            }
        }

        Set<UUID> owners = OWNERS_BY_TARGET.get(targetId);
        if (owners != null) {
            owners.remove(ownerId);
            if (owners.isEmpty()) {
                OWNERS_BY_TARGET.remove(targetId);
            }
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerWorld world : server.getWorlds()) {
            Entity found = world.getEntity(entityId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
