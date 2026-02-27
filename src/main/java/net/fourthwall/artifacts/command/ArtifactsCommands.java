package net.fourthwall.artifacts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.blood.BloodSacrificeManager;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.infested.InfestedArtifactManager;
import net.fourthwall.artifacts.poseidon.PoseidonTridentManager;
import net.fourthwall.artifacts.undead.UndeadWardArmyManager;
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
                        .executes(context -> reloadConfig(context.getSource())))
                .then(literal("undead_ward_army")
                        .then(literal("summon_deputies").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.SUMMON_DEPUTIES)))
                        .then(literal("dismiss_deputies").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.DISMISS_DEPUTIES)))
                        .then(literal("summon_commanders").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.SUMMON_COMMANDERS)))
                        .then(literal("dismiss_commanders").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.DISMISS_COMMANDERS)))
                        .then(literal("summon_warden").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.SUMMON_WARDEN)))
                        .then(literal("dismiss_warden").executes(context -> executeWardArmyCommand(context.getSource(), WardArmyAction.DISMISS_WARDEN)))));
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
        UndeadWardArmyManager.reloadConfig(server);

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

    private static int executeWardArmyCommand(ServerCommandSource source, WardArmyAction action) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException exception) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        return switch (action) {
            case SUMMON_DEPUTIES -> UndeadWardArmyManager.summonDeputies(player);
            case DISMISS_DEPUTIES -> UndeadWardArmyManager.dismissDeputies(player);
            case SUMMON_COMMANDERS -> UndeadWardArmyManager.summonCommanders(player);
            case DISMISS_COMMANDERS -> UndeadWardArmyManager.dismissCommanders(player);
            case SUMMON_WARDEN -> UndeadWardArmyManager.summonWarden(player);
            case DISMISS_WARDEN -> UndeadWardArmyManager.dismissWarden(player);
        };
    }

    private static boolean hasReloadPermission(ServerCommandSource source) {
        try {
            var player = source.getPlayerOrThrow();
            return source.getServer().getPlayerManager().isOperator(player.getPlayerConfigEntry());
        } catch (CommandSyntaxException exception) {
            return true;
        }
    }

    private enum WardArmyAction {
        SUMMON_DEPUTIES,
        DISMISS_DEPUTIES,
        SUMMON_COMMANDERS,
        DISMISS_COMMANDERS,
        SUMMON_WARDEN,
        DISMISS_WARDEN
    }
}
