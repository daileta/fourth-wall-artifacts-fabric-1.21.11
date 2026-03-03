package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArtifactEnchantments {
    private ArtifactEnchantments() {
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        if (stack.isEmpty()) {
            return false;
        }

        ArtifactsConfig config = ArtifactsConfigManager.get();
        if (config == null || config.artifactEnchants == null || config.artifactEnchants.levels == null) {
            return false;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        Map<String, Integer> configuredById = config.artifactEnchants.levels.get(itemId.toString());
        if (configuredById == null) {
            return false;
        }

        RegistryWrapper.Impl<Enchantment> enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        LinkedHashMap<RegistryEntry<Enchantment>, Integer> resolvedLevels = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : configuredById.entrySet()) {
            Identifier enchantmentId = Identifier.tryParse(entry.getKey());
            if (enchantmentId == null) {
                continue;
            }

            RegistryKey<Enchantment> enchantmentKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchantmentId);
            enchantments.getOptional(enchantmentKey).ifPresent(enchantment -> {
                int level = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
                resolvedLevels.put(enchantment, level);
            });
        }

        var before = EnchantmentHelper.getEnchantments(stack);
        var after = EnchantmentHelper.apply(stack, builder -> {
            if (config.artifactEnchants.enforceExact) {
                builder.remove(enchantment -> !resolvedLevels.containsKey(enchantment));
                resolvedLevels.forEach(builder::set);
                return;
            }

            resolvedLevels.forEach((enchantment, level) -> {
                if (level <= 0) {
                    builder.set(enchantment, 0);
                    return;
                }
                if (builder.getLevel(enchantment) < level) {
                    builder.set(enchantment, level);
                }
            });
        });

        return !after.equals(before);
    }
}
