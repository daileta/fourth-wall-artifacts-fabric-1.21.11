package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.repeater.RepeaterManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class RepeaterCrossbowItem extends CrossbowItem implements PolymerFallbackItem {
    private static final int POWER_LEVEL = 3;
    private static final int PIERCING_LEVEL = 4;
    private static final int QUICK_CHARGE_LEVEL = 5;
    private static final int UNBREAKING_LEVEL = 5;
    private static final int MENDING_LEVEL = 1;
    private static final double BASE_ARROW_DAMAGE = 6.0D;
    private static final float PROJECTILE_SPEED_MULTIPLIER = 1.15F;

    public RepeaterCrossbowItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.CROSSBOW;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            ensureEnchantments(stack, (ServerWorld) serverPlayer.getEntityWorld());
            if (!RepeaterManager.allowUse(serverPlayer, stack)) {
                return ActionResult.FAIL;
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        ensureEnchantments(stack, world);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical) {
        ProjectileEntity projectile = super.createArrowEntity(world, shooter, weaponStack, projectileStack, critical);
        if (projectile instanceof PersistentProjectileEntity persistentProjectile) {
            persistentProjectile.setDamage(BASE_ARROW_DAMAGE);
        }
        return projectile;
    }

    @Override
    public void shootAll(World world, LivingEntity shooter, Hand hand, ItemStack stack, float speed, float divergence, LivingEntity target) {
        super.shootAll(world, shooter, hand, stack, speed * PROJECTILE_SPEED_MULTIPLIER, divergence, target);
        if (!world.isClient() && shooter instanceof ServerPlayerEntity serverPlayer) {
            RepeaterManager.onShotFired(serverPlayer);
        }
    }

    private static void ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var power = enchantments.getOrThrow(Enchantments.POWER);
        var piercing = enchantments.getOrThrow(Enchantments.PIERCING);
        var quickCharge = enchantments.getOrThrow(Enchantments.QUICK_CHARGE);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);

        if (EnchantmentHelper.getLevel(power, stack) < POWER_LEVEL) {
            stack.addEnchantment(power, POWER_LEVEL);
        }
        if (EnchantmentHelper.getLevel(piercing, stack) < PIERCING_LEVEL) {
            stack.addEnchantment(piercing, PIERCING_LEVEL);
        }
        if (EnchantmentHelper.getLevel(quickCharge, stack) < QUICK_CHARGE_LEVEL) {
            stack.addEnchantment(quickCharge, QUICK_CHARGE_LEVEL);
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
        }
    }
}
