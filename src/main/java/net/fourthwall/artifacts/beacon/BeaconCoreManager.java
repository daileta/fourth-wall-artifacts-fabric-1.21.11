package net.fourthwall.artifacts.beacon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModBlocks;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BeaconCoreManager {
    private static final long HAZARD_TRACK_TTL_TICKS = 20L * 60L * 5L;
    private static final long FIRE_TRACK_TTL_TICKS = 20L * 60L * 2L;
    private static final long RESPAWN_ANCHOR_TRACK_TTL_TICKS = 40L;
    private static final long PENDING_CRYSTAL_TRACK_TTL_TICKS = 20L;
    private static final double RESPAWN_ANCHOR_MATCH_RADIUS_SQUARED = 9.0D;
    private static final double END_CRYSTAL_MATCH_RADIUS_SQUARED = 4.0D;
    private static final int HAZARD_BLOCK_CHECK_RADIUS = 1;
    private static final String STATE_KEY = "evanpack_beacon_core_anchor";
    private static final PersistentStateType<BeaconAnchorPersistentState> PERSISTENT_STATE_TYPE =
            new PersistentStateType<>(STATE_KEY, BeaconAnchorPersistentState::new, BeaconAnchorPersistentState.CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private static AnchorLocation activeAnchor;
    private static final Map<BlockHazardKey, HazardOwnerEntry> LAVA_OWNERS = new HashMap<>();
    private static final Map<BlockHazardKey, HazardOwnerEntry> FIRE_OWNERS = new HashMap<>();
    private static final Map<BlockHazardKey, HazardOwnerEntry> RESPAWN_ANCHOR_ACTIVATORS = new HashMap<>();
    private static final Map<UUID, HazardOwnerEntry> ENTITY_DAMAGE_OWNERS = new HashMap<>();
    private static final List<PendingCrystalPlacement> PENDING_END_CRYSTALS = new ArrayList<>();

    private BeaconCoreManager() {
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(BeaconCoreManager::onAllowDamage);
        ServerTickEvents.END_SERVER_TICK.register(BeaconCoreManager::onEndServerTick);
        ServerEntityEvents.ENTITY_LOAD.register(BeaconCoreManager::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> ENTITY_DAMAGE_OWNERS.remove(entity.getUuid()));
        UseBlockCallback.EVENT.register(BeaconCoreManager::onUseBlock);
        UseEntityCallback.EVENT.register(BeaconCoreManager::onUseEntity);
        AttackEntityCallback.EVENT.register(BeaconCoreManager::onAttackEntity);
        ServerLifecycleEvents.SERVER_STARTED.register(BeaconCoreManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearTransientState());
    }

    private static void onServerStarted(MinecraftServer server) {
        activeAnchor = getPersistentState(server).getAnchor();
    }

    public static void onAnchorPlaced(ServerWorld world, BlockPos pos) {
        BlockPos anchorPos = pos.toImmutable();
        MinecraftServer server = world.getServer();
        syncAnchorFromState(server);

        if (activeAnchor != null) {
            ServerWorld oldWorld = server.getWorld(activeAnchor.worldKey());
            if (oldWorld != null) {
                BlockPos oldPos = activeAnchor.pos();
                if (!oldWorld.getRegistryKey().equals(world.getRegistryKey()) || !oldPos.equals(anchorPos)) {
                    if (oldWorld.getBlockState(oldPos).isOf(ModBlocks.BEACON_ANCHOR)) {
                        oldWorld.breakBlock(oldPos, false);
                    }
                }
            }
        }

        activeAnchor = new AnchorLocation(world.getRegistryKey(), anchorPos);
        saveAnchor(server, activeAnchor);
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return true;
        }

        if (entity instanceof ServerPlayerEntity targetPlayer && isBeaconCoreProtected(targetPlayer)) {
            return false;
        }

        ServerPlayerEntity responsiblePlayer = getResponsiblePlayerForDamage(world, entity, source);
        if (responsiblePlayer != null && isBeaconCoreProtected(responsiblePlayer)) {
            return false;
        }

        return true;
    }

    private static void onEndServerTick(MinecraftServer server) {
        long tick = server.getTicks();
        pruneExpiredEntries(tick);
        if (tick % 40L == 0L) {
            pruneInvalidBlockHazards(server);
        }
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity instanceof EndCrystalEntity endCrystal) {
            PendingCrystalPlacement placement = findMatchingPendingCrystal(world, new Vec3d(endCrystal.getX(), endCrystal.getY(), endCrystal.getZ()));
            if (placement != null) {
                ENTITY_DAMAGE_OWNERS.put(endCrystal.getUuid(), new HazardOwnerEntry(placement.playerUuid(), placement.expiresAtTick() + HAZARD_TRACK_TTL_TICKS));
                PENDING_END_CRYSTALS.remove(placement);
            }
            return;
        }

        if (entity instanceof TntEntity tntEntity) {
            LivingEntity owner = tntEntity.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                ENTITY_DAMAGE_OWNERS.put(entity.getUuid(), new HazardOwnerEntry(player.getUuid(), Long.MAX_VALUE));
            }
            return;
        }

        if (entity instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                ENTITY_DAMAGE_OWNERS.put(entity.getUuid(), new HazardOwnerEntry(player.getUuid(), Long.MAX_VALUE));
            }
        }
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, net.minecraft.util.hit.BlockHitResult hitResult) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (!isBeaconCoreProtected(serverPlayer)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        BlockPos hitPos = hitResult.getBlockPos();
        BlockPos placePos = hitPos.offset(hitResult.getSide());
        long now = serverWorld.getServer().getTicks();

        if (stack.isOf(Items.LAVA_BUCKET)) {
            trackLavaPlacement(serverWorld, hitPos, serverPlayer, now);
            trackLavaPlacement(serverWorld, placePos, serverPlayer, now);
            return ActionResult.PASS;
        }

        if (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE)) {
            trackFirePlacement(serverWorld, hitPos, serverPlayer, now);
            trackFirePlacement(serverWorld, placePos, serverPlayer, now);
            BlockState clickedState = serverWorld.getBlockState(hitPos);
            if (clickedState.isOf(Blocks.TNT)) {
                // TNT ownership usually propagates to TntEntity, but we also track the primed entity when it loads.
                trackFirePlacement(serverWorld, hitPos, serverPlayer, now);
            }
            if (clickedState.isOf(Blocks.RESPAWN_ANCHOR)) {
                RESPAWN_ANCHOR_ACTIVATORS.put(new BlockHazardKey(serverWorld.getRegistryKey(), hitPos.toImmutable()), new HazardOwnerEntry(serverPlayer.getUuid(), now + RESPAWN_ANCHOR_TRACK_TTL_TICKS));
            }
            return ActionResult.PASS;
        }

        if (stack.isOf(Items.END_CRYSTAL)) {
            PENDING_END_CRYSTALS.add(new PendingCrystalPlacement(serverWorld.getRegistryKey(), placePos.toCenterPos(), serverPlayer.getUuid(), now + PENDING_CRYSTAL_TRACK_TTL_TICKS));
            return ActionResult.PASS;
        }

        if (serverWorld.getBlockState(hitPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            RESPAWN_ANCHOR_ACTIVATORS.put(new BlockHazardKey(serverWorld.getRegistryKey(), hitPos.toImmutable()), new HazardOwnerEntry(serverPlayer.getUuid(), now + RESPAWN_ANCHOR_TRACK_TTL_TICKS));
        }

        return ActionResult.PASS;
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, net.minecraft.util.hit.EntityHitResult hitResult) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (!isBeaconCoreProtected(serverPlayer)) {
            return ActionResult.PASS;
        }
        long now = serverWorld.getServer().getTicks();
        trackOwnedExplosiveEntityInteraction(serverPlayer, entity, now);
        return ActionResult.PASS;
    }

    private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, net.minecraft.util.hit.EntityHitResult hitResult) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (!isBeaconCoreProtected(serverPlayer)) {
            return ActionResult.PASS;
        }
        long now = serverWorld.getServer().getTicks();
        trackOwnedExplosiveEntityInteraction(serverPlayer, entity, now);
        return ActionResult.PASS;
    }

    private static boolean isBeaconCoreProtected(ServerPlayerEntity player) {
        if (!isHoldingBeaconCore(player)) {
            return false;
        }
        MinecraftServer server = ((ServerWorld) player.getEntityWorld()).getServer();
        if (server == null) {
            return false;
        }
        if (activeAnchor == null) {
            syncAnchorFromState(server);
        }
        if (activeAnchor == null) {
            return false;
        }
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        if (!world.getRegistryKey().equals(activeAnchor.worldKey())) {
            return false;
        }
        if (!world.getBlockState(activeAnchor.pos()).isOf(ModBlocks.BEACON_ANCHOR)) {
            clearAnchor(server);
            return false;
        }
        double radius = ArtifactsConfigManager.get().beaconCore.anchorProtectionRadius;
        return player.squaredDistanceTo(activeAnchor.pos().toCenterPos()) <= (radius * radius);
    }

    private static boolean isHoldingBeaconCore(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.BEACON_CORE) || player.getOffHandStack().isOf(ModItems.BEACON_CORE);
    }

    private static ServerPlayerEntity getResponsiblePlayerForDamage(ServerWorld world, LivingEntity target, DamageSource source) {
        ServerPlayerEntity fromEntities = resolveResponsiblePlayer(world, source.getAttacker());
        if (fromEntities != null) {
            return fromEntities;
        }

        fromEntities = resolveResponsiblePlayer(world, source.getSource());
        if (fromEntities != null) {
            return fromEntities;
        }

        ServerPlayerEntity fromEnvironment = resolveEnvironmentalHazardOwner(world, target, source);
        if (fromEnvironment != null) {
            return fromEnvironment;
        }

        return resolveExplosionPositionOwner(world, source.getPosition());
    }

    private static ServerPlayerEntity resolveResponsiblePlayer(ServerWorld world, Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof ServerPlayerEntity player) {
            return player;
        }

        if (entity instanceof ProjectileEntity projectile) {
            ServerPlayerEntity owner = resolveResponsiblePlayer(world, projectile.getOwner());
            if (owner != null) {
                return owner;
            }
        }

        if (entity instanceof TntEntity tntEntity) {
            LivingEntity owner = tntEntity.getOwner();
            if (owner != null) {
                ServerPlayerEntity player = resolveResponsiblePlayer(world, owner);
                if (player != null) {
                    return player;
                }
            }
        }

        HazardOwnerEntry tracked = ENTITY_DAMAGE_OWNERS.get(entity.getUuid());
        if (tracked != null) {
            return getServerPlayer(world.getServer(), tracked.playerUuid());
        }

        return null;
    }

    private static ServerPlayerEntity resolveEnvironmentalHazardOwner(ServerWorld world, LivingEntity target, DamageSource source) {
        Vec3d sourcePos = source.getPosition();
        if (sourcePos != null) {
            ServerPlayerEntity byExplosionPos = resolveExplosionPositionOwner(world, sourcePos);
            if (byExplosionPos != null) {
                return byExplosionPos;
            }
        }

        BlockPos base = target.getBlockPos();
        Box box = target.getBoundingBox();
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY + 0.001);

        for (int dx = -HAZARD_BLOCK_CHECK_RADIUS; dx <= HAZARD_BLOCK_CHECK_RADIUS; dx++) {
            for (int dz = -HAZARD_BLOCK_CHECK_RADIUS; dz <= HAZARD_BLOCK_CHECK_RADIUS; dz++) {
                for (int y = minY - 1; y <= maxY + 1; y++) {
                    BlockPos pos = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
                    ServerPlayerEntity lavaOwner = resolveLavaOwnerAt(world, pos);
                    if (lavaOwner != null) {
                        return lavaOwner;
                    }

                    ServerPlayerEntity fireOwner = resolveFireOwnerAt(world, pos);
                    if (fireOwner != null) {
                        return fireOwner;
                    }
                }
            }
        }

        return null;
    }

    private static ServerPlayerEntity resolveExplosionPositionOwner(ServerWorld world, Vec3d sourcePos) {
        if (sourcePos == null) {
            return null;
        }

        for (Map.Entry<BlockHazardKey, HazardOwnerEntry> entry : RESPAWN_ANCHOR_ACTIVATORS.entrySet()) {
            if (!entry.getKey().worldKey().equals(world.getRegistryKey())) {
                continue;
            }
            if (entry.getKey().pos().toCenterPos().squaredDistanceTo(sourcePos) > RESPAWN_ANCHOR_MATCH_RADIUS_SQUARED) {
                continue;
            }
            return getServerPlayer(world.getServer(), entry.getValue().playerUuid());
        }

        return null;
    }

    private static ServerPlayerEntity resolveLavaOwnerAt(ServerWorld world, BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        if (!(fluidState.isOf(Fluids.LAVA) || fluidState.isOf(Fluids.FLOWING_LAVA))) {
            return null;
        }
        HazardOwnerEntry entry = LAVA_OWNERS.get(new BlockHazardKey(world.getRegistryKey(), pos));
        return entry == null ? null : getServerPlayer(world.getServer(), entry.playerUuid());
    }

    private static ServerPlayerEntity resolveFireOwnerAt(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE))) {
            return null;
        }
        HazardOwnerEntry entry = FIRE_OWNERS.get(new BlockHazardKey(world.getRegistryKey(), pos));
        return entry == null ? null : getServerPlayer(world.getServer(), entry.playerUuid());
    }

    private static ServerPlayerEntity getServerPlayer(MinecraftServer server, UUID uuid) {
        return server.getPlayerManager().getPlayer(uuid);
    }

    private static void trackLavaPlacement(ServerWorld world, BlockPos pos, ServerPlayerEntity player, long now) {
        LAVA_OWNERS.put(new BlockHazardKey(world.getRegistryKey(), pos.toImmutable()), new HazardOwnerEntry(player.getUuid(), now + HAZARD_TRACK_TTL_TICKS));
    }

    private static void trackFirePlacement(ServerWorld world, BlockPos pos, ServerPlayerEntity player, long now) {
        FIRE_OWNERS.put(new BlockHazardKey(world.getRegistryKey(), pos.toImmutable()), new HazardOwnerEntry(player.getUuid(), now + FIRE_TRACK_TTL_TICKS));
    }

    private static void trackOwnedExplosiveEntityInteraction(ServerPlayerEntity player, Entity entity, long now) {
        if (entity instanceof TntMinecartEntity || entity instanceof EndCrystalEntity) {
            ENTITY_DAMAGE_OWNERS.put(entity.getUuid(), new HazardOwnerEntry(player.getUuid(), now + HAZARD_TRACK_TTL_TICKS));
        }
    }

    private static PendingCrystalPlacement findMatchingPendingCrystal(ServerWorld world, Vec3d pos) {
        long now = world.getServer().getTicks();
        PendingCrystalPlacement best = null;
        double bestDistance = Double.MAX_VALUE;

        for (PendingCrystalPlacement placement : PENDING_END_CRYSTALS) {
            if (placement.expiresAtTick() < now) {
                continue;
            }
            if (!placement.worldKey().equals(world.getRegistryKey())) {
                continue;
            }
            double distance = placement.pos().squaredDistanceTo(pos);
            if (distance <= END_CRYSTAL_MATCH_RADIUS_SQUARED && distance < bestDistance) {
                best = placement;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static void pruneExpiredEntries(long now) {
        pruneHazardMap(LAVA_OWNERS, now);
        pruneHazardMap(FIRE_OWNERS, now);
        pruneHazardMap(RESPAWN_ANCHOR_ACTIVATORS, now);
        ENTITY_DAMAGE_OWNERS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() < now);
        PENDING_END_CRYSTALS.removeIf(entry -> entry.expiresAtTick() < now);
    }

    private static void pruneHazardMap(Map<BlockHazardKey, HazardOwnerEntry> map, long now) {
        map.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() < now);
    }

    private static void pruneInvalidBlockHazards(MinecraftServer server) {
        pruneInvalidBlockHazards(server, LAVA_OWNERS, HazardKind.LAVA);
        pruneInvalidBlockHazards(server, FIRE_OWNERS, HazardKind.FIRE);
        pruneInvalidBlockHazards(server, RESPAWN_ANCHOR_ACTIVATORS, HazardKind.RESPAWN_ANCHOR);
    }

    private static void pruneInvalidBlockHazards(MinecraftServer server, Map<BlockHazardKey, HazardOwnerEntry> map, HazardKind kind) {
        Iterator<Map.Entry<BlockHazardKey, HazardOwnerEntry>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockHazardKey, HazardOwnerEntry> entry = iterator.next();
            ServerWorld world = server.getWorld(entry.getKey().worldKey());
            if (world == null) {
                iterator.remove();
                continue;
            }
            BlockPos pos = entry.getKey().pos();
            switch (kind) {
                case LAVA -> {
                    FluidState fluidState = world.getFluidState(pos);
                    if (!(fluidState.isOf(Fluids.LAVA) || fluidState.isOf(Fluids.FLOWING_LAVA))) {
                        iterator.remove();
                    }
                }
                case FIRE -> {
                    BlockState state = world.getBlockState(pos);
                    if (!(state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE))) {
                        iterator.remove();
                    }
                }
                case RESPAWN_ANCHOR -> {
                    if (!world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private static void clearTransientState() {
        activeAnchor = null;
        LAVA_OWNERS.clear();
        FIRE_OWNERS.clear();
        RESPAWN_ANCHOR_ACTIVATORS.clear();
        ENTITY_DAMAGE_OWNERS.clear();
        PENDING_END_CRYSTALS.clear();
    }

    private static void syncAnchorFromState(MinecraftServer server) {
        activeAnchor = getPersistentState(server).getAnchor();
    }

    private static void saveAnchor(MinecraftServer server, AnchorLocation anchor) {
        BeaconAnchorPersistentState state = getPersistentState(server);
        state.setAnchor(anchor);
    }

    private static void clearAnchor(MinecraftServer server) {
        activeAnchor = null;
        BeaconAnchorPersistentState state = getPersistentState(server);
        state.clearAnchor();
    }

    private static BeaconAnchorPersistentState getPersistentState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(PERSISTENT_STATE_TYPE);
    }

    private record AnchorLocation(RegistryKey<World> worldKey, BlockPos pos) {
    }

    private record BlockHazardKey(RegistryKey<World> worldKey, BlockPos pos) {
    }

    private record HazardOwnerEntry(UUID playerUuid, long expiresAtTick) {
    }

    private record PendingCrystalPlacement(RegistryKey<World> worldKey, Vec3d pos, UUID playerUuid, long expiresAtTick) {
    }

    private enum HazardKind {
        LAVA,
        FIRE,
        RESPAWN_ANCHOR
    }

    private static final class BeaconAnchorPersistentState extends PersistentState {
        private static final Codec<BeaconAnchorPersistentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("has_anchor", false).forGetter(state -> state.anchor != null),
                        Codec.STRING.optionalFieldOf("dimension", "").forGetter(state -> state.anchor == null ? "" : state.anchor.worldKey().getValue().toString()),
                        Codec.INT.optionalFieldOf("x", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos().getX()),
                        Codec.INT.optionalFieldOf("y", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos().getY()),
                        Codec.INT.optionalFieldOf("z", 0).forGetter(state -> state.anchor == null ? 0 : state.anchor.pos().getZ())
                )
                .apply(instance, BeaconAnchorPersistentState::fromCodec));

        private AnchorLocation anchor;

        private BeaconAnchorPersistentState() {
        }

        private static BeaconAnchorPersistentState fromCodec(boolean hasAnchor, String dimensionValue, int x, int y, int z) {
            BeaconAnchorPersistentState state = new BeaconAnchorPersistentState();
            if (!hasAnchor || dimensionValue.isEmpty()) {
                return state;
            }

            Identifier dimensionId = Identifier.tryParse(dimensionValue);
            if (dimensionId == null) {
                return state;
            }

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
            BlockPos pos = new BlockPos(x, y, z);
            state.anchor = new AnchorLocation(worldKey, pos);
            return state;
        }

        private AnchorLocation getAnchor() {
            return anchor;
        }

        private void setAnchor(AnchorLocation anchor) {
            if (this.anchor != null && this.anchor.equals(anchor)) {
                return;
            }
            this.anchor = anchor;
            markDirty();
        }

        private void clearAnchor() {
            if (anchor == null) {
                return;
            }
            anchor = null;
            markDirty();
        }
    }
}
