package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.repeater.RepeaterManager;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.text.Text;

public class RepeaterCrossbowItem extends CrossbowItem implements PolymerFallbackItem {
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
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("T").styled(s -> s.withColor(0xFFE229).withBold(true)))
            .append(Text.literal("h").styled(s -> s.withColor(0xFFDC29).withBold(true)))
            .append(Text.literal("e ").styled(s -> s.withColor(0xFFD629).withBold(true)))
            .append(Text.literal("R").styled(s -> s.withColor(0xFFD129).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFFCB29).withBold(true)))
            .append(Text.literal("p").styled(s -> s.withColor(0xFFC529).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFFBF29).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xFFB929).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0xFFB329).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFFAE29).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFA229).withBold(true)));
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
        refreshConfiguredStack(stack, world);
    }

    @Override
    protected ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical) {
        ProjectileEntity projectile = super.createArrowEntity(world, shooter, weaponStack, projectileStack, critical);
        if (projectile instanceof PersistentProjectileEntity persistentProjectile) {
            persistentProjectile.setDamage(BASE_ARROW_DAMAGE);
            RepeaterManager.markRepeaterArrow(persistentProjectile);
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

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
