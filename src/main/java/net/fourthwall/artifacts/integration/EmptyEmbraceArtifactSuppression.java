package net.fourthwall.artifacts.integration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class EmptyEmbraceArtifactSuppression {
    public static final String EMPTY_EMBRACE_TAG = "magic.empty_embrace";
    public static final String ARTIFACT_POWERS_TAG = "magic.empty_embrace.artifact_powers";
    public static final String ARTIFACT_SUMMONS_TAG = "magic.empty_embrace.artifact_summons";
    public static final String ARTIFACT_ARMOR_TAG = "magic.empty_embrace.artifact_armor";
    public static final String INFESTED_SILVERFISH_TAG = "magic.empty_embrace.infested_silverfish";

    private EmptyEmbraceArtifactSuppression() {
    }

    public static boolean isEmptyEmbraced(PlayerEntity player) {
        return hasTag(player, EMPTY_EMBRACE_TAG);
    }

    public static boolean areArtifactPowersSuppressed(PlayerEntity player) {
        return hasTag(player, ARTIFACT_POWERS_TAG);
    }

    public static boolean areArtifactSummonsSuppressed(PlayerEntity player) {
        return hasTag(player, ARTIFACT_SUMMONS_TAG);
    }

    public static boolean isArtifactArmorSuppressed(PlayerEntity player) {
        return hasTag(player, ARTIFACT_ARMOR_TAG);
    }

    public static boolean areInfestedSilverfishSuppressed(PlayerEntity player) {
        return hasTag(player, INFESTED_SILVERFISH_TAG);
    }

    private static boolean hasTag(Entity entity, String tag) {
        return entity != null && entity.getCommandTags().contains(tag);
    }
}
