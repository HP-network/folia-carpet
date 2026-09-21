package carpet.utils;

import carpet.CarpetSettings;
import carpet.folia.FoliaRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class CommandHelper {
    private CommandHelper() {}

    public static void notifyPlayersCommandsChanged(MinecraftServer server)
    {
        if (server == null || server.getPlayerList() == null)
        {
            return;
        }
        try
        {
            for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers()))
            {
                FoliaRuntime.runOnPlayer(player, target -> server.getCommands().sendCommands(target));
            }
        }
        catch (NullPointerException e)
        {
            CarpetSettings.LOG.warn("Exception while refreshing commands, please report this to Carpet", e);
        }
    }

    public static boolean canUseCommand(CommandSourceStack source, Object commandLevel)
    {
        if (commandLevel instanceof Boolean) return (Boolean) commandLevel;
        String commandLevelString = commandLevel.toString();
        return switch (commandLevelString)
        {
            case "true"  -> true;
            case "false" -> false;
            case "ops"   -> Commands.LEVEL_GAMEMASTERS.check(source.permissions());
            case "0" ->  Commands.LEVEL_ALL.check(source.permissions());
            case "1" -> Commands.LEVEL_MODERATORS.check(source.permissions());
            case "2" -> Commands.LEVEL_GAMEMASTERS.check(source.permissions());
            case "3" -> Commands.LEVEL_ADMINS.check(source.permissions());
            case "4" -> Commands.LEVEL_OWNERS.check(source.permissions());
            default -> false;
        };
    }
}
