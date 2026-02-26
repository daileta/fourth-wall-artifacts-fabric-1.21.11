package net.fourthwall.artifacts.command;

import net.fourthwall.artifacts.item.InfestedPickaxeItem;
import net.fourthwall.artifacts.item.InfestedSwordItem;
import net.fourthwall.artifacts.item.LionsHeartItem;
import net.fourthwall.artifacts.item.PolymerFallbackItem;
import net.fourthwall.artifacts.item.RepeaterCrossbowItem;
import net.fourthwall.artifacts.item.SmolderingRodItem;
import net.fourthwall.artifacts.item.TridentOfPoseidonItem;
import net.fourthwall.artifacts.item.VoidReaverItem;
import net.fourthwall.artifacts.item.EarthsplitterItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

public final class ArtifactReloadApplier {
    private ArtifactReloadApplier() {
    }

    public static ReloadSummary applyToLoadedWorld(MinecraftServer server) {
        int refreshedStacks = 0;
        int inventoriesTouched = 0;
        int itemEntitiesRefreshed = 0;
        int equipmentStacksRefreshed = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            RefreshCount main = refreshInventory((ServerWorld) player.getEntityWorld(), player.getInventory());
            refreshedStacks += main.refreshedStacks();
            if (main.touched()) {
                inventoriesTouched++;
            }

            RefreshCount ender = refreshInventory((ServerWorld) player.getEntityWorld(), player.getEnderChestInventory());
            refreshedStacks += ender.refreshedStacks();
            if (ender.touched()) {
                inventoriesTouched++;
            }
        }

        for (ServerWorld world : server.getWorlds()) {
            final int[] blockEntityCounters = new int[2];
            world.getChunkManager().chunkLoadingManager.forEachChunk(chunk -> {
                RefreshCount count = refreshBlockEntityInventoriesInChunk(world, chunk);
                blockEntityCounters[0] += count.refreshedStacks();
                if (count.touched()) {
                    blockEntityCounters[1]++;
                }
            });
            refreshedStacks += blockEntityCounters[0];
            inventoriesTouched += blockEntityCounters[1];

            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof ServerPlayerEntity) {
                    continue;
                }

                if (entity instanceof ItemEntity itemEntity) {
                    if (refreshStack(world, itemEntity.getStack())) {
                        itemEntity.setStack(itemEntity.getStack());
                        refreshedStacks++;
                        itemEntitiesRefreshed++;
                    }
                }

                if (entity instanceof ItemFrameEntity itemFrame) {
                    ItemStack held = itemFrame.getHeldItemStack();
                    if (refreshStack(world, held)) {
                        itemFrame.setHeldItemStack(held, true);
                        refreshedStacks++;
                    }
                }

                if (entity instanceof Inventory inventory) {
                    RefreshCount count = refreshInventory(world, inventory);
                    refreshedStacks += count.refreshedStacks();
                    if (count.touched()) {
                        inventoriesTouched++;
                    }
                }

                if (entity instanceof LivingEntity living) {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack equipped = living.getEquippedStack(slot);
                        if (refreshStack(world, equipped)) {
                            living.equipStack(slot, equipped);
                            refreshedStacks++;
                            equipmentStacksRefreshed++;
                        }
                    }
                }
            }
        }

        return new ReloadSummary(refreshedStacks, inventoriesTouched, itemEntitiesRefreshed, equipmentStacksRefreshed);
    }

    private static RefreshCount refreshInventory(ServerWorld world, Inventory inventory) {
        int refreshedStacks = 0;
        boolean touched = false;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (refreshStack(world, stack)) {
                inventory.setStack(slot, stack);
                refreshedStacks++;
                touched = true;
            }
        }
        if (touched) {
            inventory.markDirty();
        }
        return new RefreshCount(refreshedStacks, touched);
    }

    private static RefreshCount refreshBlockEntityInventoriesInChunk(ServerWorld world, WorldChunk chunk) {
        int refreshedStacks = 0;
        boolean touched = false;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof Inventory inventory)) {
                continue;
            }
            RefreshCount count = refreshInventory(world, inventory);
            refreshedStacks += count.refreshedStacks();
            touched |= count.touched();
        }
        return new RefreshCount(refreshedStacks, touched);
    }

    private static boolean refreshStack(ServerWorld world, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PolymerFallbackItem)) {
            return false;
        }

        if (stack.getItem() instanceof LionsHeartItem) {
            return LionsHeartItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof TridentOfPoseidonItem) {
            return TridentOfPoseidonItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof RepeaterCrossbowItem) {
            return RepeaterCrossbowItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof SmolderingRodItem) {
            return SmolderingRodItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof InfestedSwordItem) {
            return InfestedSwordItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof InfestedPickaxeItem) {
            return InfestedPickaxeItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof VoidReaverItem) {
            return VoidReaverItem.refreshConfiguredStack(stack, world);
        }
        if (stack.getItem() instanceof EarthsplitterItem) {
            return EarthsplitterItem.refreshConfiguredStack(stack, world);
        }

        return false;
    }

    private record RefreshCount(int refreshedStacks, boolean touched) {
    }

    public record ReloadSummary(int refreshedStacks, int inventoriesTouched, int itemEntitiesRefreshed, int equipmentStacksRefreshed) {
    }
}
