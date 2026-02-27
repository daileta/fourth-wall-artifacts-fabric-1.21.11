package net.fourthwall.artifacts.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public interface PolymerFallbackItem extends PolymerItem {
    Item getFallbackItem(ItemStack stack);

    @Override
    default Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return this.getFallbackItem(itemStack);
    }

    @Override
    @Nullable
    default Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        // Keep dedicated-server behavior strict for vanilla clients, but allow integrated singleplayer
        // to use local mod assets without requiring a generated/served Polymer pack.
        boolean hasMainPack = PolymerResourcePackUtils.hasMainPack(context);
        ServerPlayerEntity player = context.getPlayer();
        boolean integratedSingleplayer = player != null
                && player.getEntityWorld().getServer() != null
                && !player.getEntityWorld().getServer().isDedicated();

        if (!hasMainPack && !integratedSingleplayer) {
            return null;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (hasMainPack && !PolymerPackAssetGuard.hasItemDefinition(itemId)) {
            return null;
        }
        return itemId;
    }
}
