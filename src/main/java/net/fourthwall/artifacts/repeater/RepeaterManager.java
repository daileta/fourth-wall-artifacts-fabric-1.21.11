package net.fourthwall.artifacts.repeater;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RepeaterManager {
    private static final int STARTUP_TICKS = 60;
    // Small grace window so rapid re-charge shots count as one firing burst.
    private static final int STOP_FIRING_GRACE_TICKS = 8;

    private static final Identifier FIRING_SLOWDOWN_ID = FourthWallArtifacts.id("repeater_firing_slowdown");
    private static final EntityAttributeModifier FIRING_SLOWDOWN = new EntityAttributeModifier(
            FIRING_SLOWDOWN_ID,
            -0.5D,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static final Map<UUID, RepeaterState> STATES = new HashMap<>();

    private RepeaterManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RepeaterManager::onEndServerTick);
    }

    public static boolean allowUse(ServerPlayerEntity player, ItemStack stack) {
        RepeaterState state = STATES.computeIfAbsent(player.getUuid(), ignored -> new RepeaterState());
        long now = ((ServerWorld) player.getEntityWorld()).getTime();
        if (state.activeFiring) {
            return true;
        }

        long readyAt = Math.max(state.startupReadyTick, state.cooldownReadyTick);
        if (now >= readyAt) {
            return true;
        }

        setCooldownVisual(player, stack, readyAt - now);
        return false;
    }

    public static void onShotFired(ServerPlayerEntity player) {
        RepeaterState state = STATES.computeIfAbsent(player.getUuid(), ignored -> new RepeaterState());
        long now = ((ServerWorld) player.getEntityWorld()).getTime();
        state.activeFiring = true;
        state.lastShotTick = now;
        state.startupReadyTick = 0L;
        state.cooldownReadyTick = 0L;
        applyFiringSlowdown(player);
    }

    private static void onEndServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        RepeaterState state = STATES.computeIfAbsent(playerId, ignored -> new RepeaterState());
        long now = ((ServerWorld) player.getEntityWorld()).getTime();

        ItemStack heldRepeater = getHeldRepeaterStack(player);
        boolean holdingRepeater = !heldRepeater.isEmpty();

        if (holdingRepeater && !state.wasHoldingRepeater) {
            state.startupReadyTick = Math.max(state.startupReadyTick, now + STARTUP_TICKS);
            setCooldownVisual(player, heldRepeater, Math.max(0L, state.startupReadyTick - now));
        }

        if (!holdingRepeater && state.activeFiring) {
            stopFiring(player, state, now, ItemStack.EMPTY);
        } else if (state.activeFiring) {
            if (now - state.lastShotTick > STOP_FIRING_GRACE_TICKS) {
                stopFiring(player, state, now, heldRepeater);
            } else {
                applyFiringSlowdown(player);
            }
        } else {
            removeFiringSlowdown(player);
        }

        state.wasHoldingRepeater = holdingRepeater;

        if (!holdingRepeater && !state.activeFiring && now >= state.cooldownReadyTick && now >= state.startupReadyTick) {
            STATES.remove(playerId);
        }
    }

    private static void stopFiring(ServerPlayerEntity player, RepeaterState state, long now, ItemStack heldRepeater) {
        int cooldownTicks = ArtifactsConfigManager.get().repeater.postFiringCooldownTicks;
        state.activeFiring = false;
        state.cooldownReadyTick = now + cooldownTicks;
        state.startupReadyTick = 0L;
        removeFiringSlowdown(player);
        if (!heldRepeater.isEmpty()) {
            setCooldownVisual(player, heldRepeater, cooldownTicks);
        }
    }

    private static ItemStack getHeldRepeaterStack(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(ModItems.REPEATER)) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().isOf(ModItems.REPEATER)) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private static void setCooldownVisual(ServerPlayerEntity player, ItemStack stack, long ticks) {
        if (stack.isEmpty() || ticks <= 0L) {
            return;
        }
        player.getItemCooldownManager().set(stack, (int) Math.min(Integer.MAX_VALUE, ticks));
    }

    private static void applyFiringSlowdown(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeed == null || movementSpeed.hasModifier(FIRING_SLOWDOWN_ID)) {
            return;
        }
        movementSpeed.addTemporaryModifier(FIRING_SLOWDOWN);
    }

    private static void removeFiringSlowdown(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        movementSpeed.removeModifier(FIRING_SLOWDOWN_ID);
    }

    private static final class RepeaterState {
        private boolean wasHoldingRepeater;
        private boolean activeFiring;
        private long lastShotTick;
        private long startupReadyTick;
        private long cooldownReadyTick;
    }
}
