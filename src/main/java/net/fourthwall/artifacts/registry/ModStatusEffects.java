package net.fourthwall.artifacts.registry;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.infested.GuaranteedInfestedStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModStatusEffects {
    public static final RegistryEntry.Reference<StatusEffect> GUARANTEED_INFESTED = register(
            "guaranteed_infested",
            new GuaranteedInfestedStatusEffect()
    );

    private ModStatusEffects() {
    }

    public static void init() {
        // Triggers static registration.
    }

    private static RegistryEntry.Reference<StatusEffect> register(String path, StatusEffect effect) {
        Identifier id = FourthWallArtifacts.id(path);
        return Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
    }
}
