package net.fourthwall.artifacts.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;


public class InfestedPickaxeItem extends Item implements PolymerFallbackItem {
    private static final float BASE_MINING_SPEED = 26.0F;

    public InfestedPickaxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_PICKAXE;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("I").styled(s -> s.withColor(0xA69FA3).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0xA09A9D).withBold(true)))
            .append(Text.literal("f").styled(s -> s.withColor(0x9B9698).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x959192).withBold(true)))
            .append(Text.literal("s").styled(s -> s.withColor(0x8F8C8D).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x898887).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x848381).withBold(true)))
            .append(Text.literal("d ").styled(s -> s.withColor(0x7E7E7C).withBold(true)))
            .append(Text.literal("P").styled(s -> s.withColor(0x787A76).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x727571).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0x6D706B).withBold(true)))
            .append(Text.literal("k").styled(s -> s.withColor(0x676C65).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0x616760).withBold(true)))
            .append(Text.literal("x").styled(s -> s.withColor(0x5B625A).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x50594F).withBold(true)));
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return BASE_MINING_SPEED;
        }
        return super.getMiningSpeed(stack, state);
    }

    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState targetBlock, BlockPos pos, ServerPlayerEntity player) {
        return true;
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
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
