package net.fourthwall.artifacts.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
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
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (!PolymerPackAssetGuard.hasItemDefinition(itemId)) {
            return null;
        }
        return itemId;
    }
}
