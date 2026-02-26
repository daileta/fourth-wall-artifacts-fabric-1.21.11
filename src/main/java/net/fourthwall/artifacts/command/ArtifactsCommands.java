package net.fourthwall.artifacts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.blood.BloodSacrificeManager;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.infested.InfestedArtifactManager;
import net.fourthwall.artifacts.poseidon.PoseidonTridentManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class ArtifactsCommands {
    private ArtifactsCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register(ArtifactsCommands::register);
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("artifacts")
                .then(literal("reload_config")
                        .executes(context -> reloadConfig(context.getSource()))));
    }

    private static int reloadConfig(ServerCommandSource source) {
        var server = source.getServer();
        if (!hasReloadPermission(source)) {
            source.sendError(Text.literal("You must be an operator to use this command."));
            return 0;
        }

        ArtifactsConfigManager.load(FourthWallArtifacts.LOGGER);
        InfestedArtifactManager.reloadConfig();
        PoseidonTridentManager.reloadConfig(server);
        BloodSacrificeManager.reloadConfig(server);

        ArtifactReloadApplier.ReloadSummary summary = ArtifactReloadApplier.applyToLoadedWorld(server);

        source.sendFeedback(() -> Text.literal(
                "Artifacts config reloaded. Refreshed " + summary.refreshedStacks() + " artifact stack(s), " +
                        summary.inventoriesTouched() + " inventory(s), " +
                        summary.itemEntitiesRefreshed() + " dropped item(s), " +
                        summary.equipmentStacksRefreshed() + " equipped stack(s)."
        ), true);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.currentScreenHandler.sendContentUpdates();
            player.playerScreenHandler.sendContentUpdates();
        }

        return summary.refreshedStacks();
    }

    private static boolean hasReloadPermission(ServerCommandSource source) {
        try {
            var player = source.getPlayerOrThrow();
            return source.getServer().getPlayerManager().isOperator(player.getPlayerConfigEntry());
        } catch (CommandSyntaxException exception) {
            return true;
        }
    }
}
