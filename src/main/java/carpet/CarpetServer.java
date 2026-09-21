package carpet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import carpet.commands.CounterCommand;
import carpet.commands.DistanceCommand;
import carpet.commands.DrawCommand;
import carpet.commands.InfoCommand;
import carpet.commands.LogCommand;
import carpet.commands.MobAICommand;
import carpet.commands.PerimeterInfoCommand;
import carpet.commands.PlayerCommand;
import carpet.commands.ProfileCommand;
import carpet.script.ScriptCommand;
import carpet.commands.SpawnCommand;
import carpet.commands.TestCommand;
import carpet.network.ServerNetworkHandler;
import carpet.helpers.HopperCounter;
import carpet.logging.LoggerRegistry;
import carpet.script.CarpetScriptServer;
import carpet.script.CarpetEventServer;
import carpet.api.settings.SettingsManager;
import carpet.logging.HUDController;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.utils.ParticleParser;
import carpet.utils.MobAI;
import carpet.folia.PlatformCompat;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

public class CarpetServer
{
    public static MinecraftServer minecraft_server;
    public static CarpetScriptServer scriptServer;
    public static carpet.settings.SettingsManager settingsManager;
    public static final List<CarpetExtension> extensions = new ArrayList<>();

    private static final Object LIFECYCLE_LOCK = new Object();
    private static MinecraftServer lifecycleServer;
    private static boolean worldsInitialized;
    private static CommandDispatcher<CommandSourceStack> registeredDispatcher;

    public static void manageExtension(CarpetExtension extension)
    {
        extensions.add(extension);

        if (StackWalker.getInstance().walk(stream -> stream.skip(1)
                .anyMatch(el -> el.getClassName() == CarpetServer.class.getName())))
        {
            CarpetSettings.LOG.warn("""
                    Extension '%s' is registering itself using a mixin into Carpet instead of a regular ModInitializer!
                    This is stupid and will crash the game in future versions!""".formatted(extension.getClass().getSimpleName()));
        }
    }

    public static void onGameStarted()
    {
        settingsManager = new carpet.settings.SettingsManager(CarpetSettings.carpetVersion, "carpet", "Carpet Mod");
        settingsManager.parseSettingsClass(CarpetSettings.class);
        extensions.forEach(CarpetExtension::onGameStarted);

        CarpetScriptServer.parseFunctionClasses();
    }

    public static void onServerLoaded(MinecraftServer server)
    {
        synchronized (LIFECYCLE_LOCK)
        {
            if (server == null || (lifecycleServer == server && scriptServer != null))
            {
                return;
            }
            lifecycleServer = server;
            worldsInitialized = false;
            CarpetServer.minecraft_server = server;

            SpawnReporter.resetSpawnStats(server, true);

            forEachManager(sm -> sm.attachServer(server));
            extensions.forEach(e -> e.onServerLoaded(server));
            scriptServer = new CarpetScriptServer(server);
            Carpet.MinecraftServer_addScriptServer(server, scriptServer);
            MobAI.resetTrackers();
            LoggerRegistry.initLoggers();
        }

    }

    public static void onServerLoadedWorlds(MinecraftServer minecraftServer)
    {
        synchronized (LIFECYCLE_LOCK)
        {
            if (minecraftServer == null || minecraft_server != minecraftServer
                    || scriptServer == null || worldsInitialized)
            {
                return;
            }
            HopperCounter.resetAll(minecraftServer, true);
            extensions.forEach(e -> e.onServerLoadedWorlds(minecraftServer));

            forEachManager(SettingsManager::initializeScarpetRules);
            scriptServer.initializeForWorld();
            worldsInitialized = true;
        }
    }

    public static void reloadWorlds(MinecraftServer server)
    {
        synchronized (LIFECYCLE_LOCK)
        {
            if (server == minecraft_server)
            {
                worldsInitialized = false;
            }
        }
        onServerLoadedWorlds(server);
    }

    public static void tick(MinecraftServer server)
    {
        if (server == null || server != minecraft_server)
        {
            return;
        }
        HUDController.update_hud(server, null);
        if (scriptServer != null) scriptServer.tick();

        // The global event is safe to dispatch from Folia's global scheduler.
        // Dimension-specific events are scheduled on their own region by the
        // Folia plugin so Scarpet callbacks can touch that world safely.
        if (server.tickRateManager().runsNormally())
        {
            CarpetEventServer.Event.TICK.onTick(server);
        }

        CarpetSettings.impendingFillSkipUpdates.set(false);
        extensions.forEach(e -> e.onTick(server));

    }

    public static void tickDimensionEvent(MinecraftServer server, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension)
    {
        if (server == null || server != minecraft_server || !server.tickRateManager().runsNormally())
        {
            return;
        }
        if (dimension == net.minecraft.world.level.Level.NETHER)
        {
            CarpetEventServer.Event.NETHER_TICK.onTick(server);
        }
        else if (dimension == net.minecraft.world.level.Level.END)
        {
            CarpetEventServer.Event.ENDER_TICK.onTick(server);
        }

    }

    public static synchronized void registerCarpetCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext commandBuildContext)
    {
        if (settingsManager == null || dispatcher == null || registeredDispatcher == dispatcher)
        {
            return;
        }
        registeredDispatcher = dispatcher;
        forEachManager(sm -> sm.registerCommand(dispatcher, commandBuildContext));

        ProfileCommand.register(dispatcher, commandBuildContext);
        CounterCommand.register(dispatcher, commandBuildContext);
        LogCommand.register(dispatcher, commandBuildContext);
        SpawnCommand.register(dispatcher, commandBuildContext);
        PlayerCommand.register(dispatcher, commandBuildContext);
        InfoCommand.register(dispatcher, commandBuildContext);
        DistanceCommand.register(dispatcher, commandBuildContext);
        PerimeterInfoCommand.register(dispatcher, commandBuildContext);
        DrawCommand.register(dispatcher, commandBuildContext);
        ScriptCommand.register(dispatcher, commandBuildContext);
        MobAICommand.register(dispatcher, commandBuildContext);

        extensions.forEach(e -> {
            e.registerCommands(dispatcher, commandBuildContext);
        });

        if (environment != Commands.CommandSelection.DEDICATED)
            PerfCommand.register(dispatcher);

        if (PlatformCompat.isDevelopmentEnvironment())
            TestCommand.register(dispatcher);

    }

    public static void onPlayerLoggedIn(ServerPlayer player)
    {
        if (player == null || scriptServer == null)
        {
            return;
        }
        ServerNetworkHandler.onPlayerJoin(player);
        LoggerRegistry.playerConnected(player);
        extensions.forEach(e -> e.onPlayerLoggedIn(player));
        scriptServer.onPlayerJoin(player);
    }

    public static void onPlayerLoggedOut(ServerPlayer player, Component reason)
    {
        ServerNetworkHandler.onPlayerLoggedOut(player);
        LoggerRegistry.playerDisconnected(player);
        extensions.forEach(e -> e.onPlayerLoggedOut(player));

        CarpetScriptServer runningScriptServer = (player.level().getServer() == null) ? scriptServer : Vanilla.MinecraftServer_getScriptServer(player.level().getServer());
        if (runningScriptServer != null && !runningScriptServer.stopAll) {
            runningScriptServer.onPlayerLoggedOut(player, reason);
        }
    }

    public static void clientPreClosing()
    {
        if (scriptServer != null) scriptServer.onClose();
        scriptServer = null;
    }

    public static void onServerClosed(@Nullable MinecraftServer server)
    {
        synchronized (LIFECYCLE_LOCK)
        {
            if (minecraft_server == null)
            {
                return;
            }
            MinecraftServer activeServer = minecraft_server;
            if (scriptServer != null) scriptServer.onClose();

            CarpetScriptServer externalScriptServer = server == null ? null : Vanilla.MinecraftServer_getScriptServer(server);
            if (externalScriptServer != null && externalScriptServer != scriptServer && !externalScriptServer.stopAll) {
                externalScriptServer.onClose();
            }

            scriptServer = null;
            ServerNetworkHandler.close();

            LoggerRegistry.stopLoggers();
            HUDController.resetScarpetHUDs();
            ParticleParser.resetCache();
            extensions.forEach(e -> e.onServerClosed(activeServer));
            minecraft_server = null;
            lifecycleServer = null;
            worldsInitialized = false;
            registeredDispatcher = null;
            if (server == null || server == activeServer)
            {
                scriptServer = null;
            }
        }
    }
    public static void onServerDoneClosing(MinecraftServer server)
    {
        forEachManager(SettingsManager::detachServer);
    }

    public static void forEachManager(Consumer<SettingsManager> consumer)
    {
        consumer.accept(settingsManager);
        for (CarpetExtension e : extensions)
        {
            SettingsManager manager = e.extensionSettingsManager();
            if (manager != null)
            {
                consumer.accept(manager);
            }
        }
    }

    public static void registerExtensionLoggers()
    {
        extensions.forEach(CarpetExtension::registerLoggers);
    }

    public static void onReload(MinecraftServer server)
    {
        scriptServer.reload(server);
        extensions.forEach(e -> e.onReload(server));
    }
}
