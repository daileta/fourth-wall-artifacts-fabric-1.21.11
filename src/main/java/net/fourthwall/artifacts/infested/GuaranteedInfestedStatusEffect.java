package net.fourthwall.artifacts.infested;

import eu.pb4.polymer.core.api.other.PolymerStatusEffect;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.packettweaker.PacketContext;

public class GuaranteedInfestedStatusEffect extends StatusEffect implements PolymerStatusEffect {
    private static final int COLOR = 0x8CA4CC;

    public GuaranteedInfestedStatusEffect() {
        super(StatusEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public StatusEffect getPolymerReplacement(StatusEffect potion, PacketContext context) {
        return StatusEffects.INFESTED.value();
    }

    @Override
    public void onEntityDamage(ServerWorld world, LivingEntity entity, int amplifier, net.minecraft.entity.damage.DamageSource source, float amount) {
        int silverfishCount = MathHelper.nextBetween(entity.getRandom(), 1, 2);

        for (int i = 0; i < silverfishCount; i++) {
            spawnSilverfish(world, entity);
        }
    }

    private static void spawnSilverfish(ServerWorld world, LivingEntity entity) {
        SilverfishEntity silverfish = EntityType.SILVERFISH.create(world, SpawnReason.TRIGGERED);
        if (silverfish == null) {
            return;
        }

        float yawOffset = MathHelper.nextBetween(entity.getRandom(), -1.5707964F, 1.5707964F);
        Vec3d velocity = entity.getRotationVector().multiply(0.3D).multiply(1.0D, 1.5D, 1.0D).rotateY(yawOffset);

        silverfish.refreshPositionAndAngles(
                entity.getX(),
                entity.getY() + entity.getHeight() * 0.5D,
                entity.getZ(),
                world.getRandom().nextFloat() * 360.0F,
                0.0F
        );
        silverfish.setVelocity(velocity);
        world.spawnEntity(silverfish);
    }
}
