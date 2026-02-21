package net.fourthwall.artifacts;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FourthWallArtifacts implements ModInitializer {
	public static final String MOD_ID = "artifacts";
	private static final Identifier SMOLDERING_ROD_ID = Identifier.of("evanpack", "smoldering_rod");
	public static final Item SMOLDERING_ROD = Registry.register(
			Registries.ITEM,
			SMOLDERING_ROD_ID,
			new SmolderingRod(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, SMOLDERING_ROD_ID))
					.maxDamage(128))
	);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SmolderingRodConfig config = SmolderingRodConfig.load(LOGGER);
		SmolderingRodManager.init(config);
	}
}
