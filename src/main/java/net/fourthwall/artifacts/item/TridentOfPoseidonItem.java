package net.fourthwall.artifacts.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.AttributeModifierSlot;

public class TridentOfPoseidonItem extends TridentItem implements PolymerFallbackItem {
    private static final int RiptideLevel = 3;
    private static final int UNBREAKING_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;
    private static final int IMPALING_LEVEL = 5;

    public TridentOfPoseidonItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.TRIDENT;
    }

    public static AttributeModifiersComponent createPoseidonAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 11.0D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -2.9D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public ItemStack getDefaultStack() {
        return super.getDefaultStack();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var riptide = enchantments.getOrThrow(Enchantments.RIPTIDE);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var impaling = enchantments.getOrThrow(Enchantments.IMPALING);
        boolean changed = false;
        if (EnchantmentHelper.getLevel(riptide, stack) < RiptideLevel) {
            stack.addEnchantment(riptide, RiptideLevel);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(impaling, stack) < IMPALING_LEVEL) {
            stack.addEnchantment(impaling, IMPALING_LEVEL);
            changed = true;
        }
        return changed;
    }
}
