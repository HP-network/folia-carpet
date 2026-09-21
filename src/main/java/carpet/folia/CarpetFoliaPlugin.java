package carpet.folia;

import carpet.CarpetServer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.event.player.PlayerFailMoveEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class CarpetFoliaPlugin extends JavaPlugin implements Listener {

    private static CarpetFoliaPlugin instance;

    public static int getTick()
    {
        return FoliaRuntime.tick();
    }

    private final AtomicReference<ServerPlayer> lastQuitPlayer = new AtomicReference<>();
    private final List<ScheduledTask> dimensionTickTasks = new CopyOnWriteArrayList<>();
    private MinecraftServer server;

    @Override
    public void onLoad() {
        instance = this;
        FoliaRuntime.bind(this);
        PlatformCompat.init(this);
        CarpetServer.onGameStarted();
        getLogger().info("Carpet Folia initialized (mixins: "
                + CarpetMixinBridge.transformedClasses() + " transformed, "
                + CarpetMixinBridge.failedTransformations() + " failed, "
                + CarpetMixinBridge.serverClassInvocations() + "/"
                + CarpetMixinBridge.transformerInvocations() + " server classes seen; configs: "
                + CarpetMixinBridge.mixinSummary() + ")");
    }

    @Override
    public void onEnable() {
        try {
            this.server = ((CraftServer) Bukkit.getServer()).getServer();

            CarpetServer.onServerLoaded(server);
            registerCarpetCommands();

            Bukkit.getPluginManager().registerEvents(this, this);
            ChunkRegistry.register(this);
            ParrotFolia.attach(this);
            TntFolia tntFolia = new TntFolia();
            ParrotFolia parrotFolia = new ParrotFolia();
            ShulkerFolia shulkerFolia = new ShulkerFolia();
            RailFolia railFolia = new RailFolia(this);
            Bukkit.getPluginManager().registerEvents(tntFolia, this);
            Bukkit.getPluginManager().registerEvents(parrotFolia, this);
            Bukkit.getPluginManager().registerEvents(shulkerFolia, this);
            Bukkit.getPluginManager().registerEvents(railFolia, this);

            Bukkit.getGlobalRegionScheduler().run(this, task -> CarpetServer.onServerLoadedWorlds(server));
            Bukkit.getGlobalRegionScheduler().run(this, task -> scheduleDimensionTicks());

            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                FoliaRuntime.advanceTick();
                try
                {
                    CarpetServer.tick(server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in Carpet per-tick update: " + t);
                }
                try
                {
                    MixinCompat.player_tickActionPacks(server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in Carpet action pack tick: " + t);
                }
                try
                {
                    MovableBlockEntities.tickScan(this, server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in movable block entity scan: " + t);
                }
                try
                {
                    RailFolia.tick(this, server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in rail power limit scan: " + t);
                }
                try
                {
                    TntFolia.tickMergeScan(this, server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in TNT scan: " + t);
                }
                try
                {
                    TntFolia.tickCleanup();
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in TNT placement cleanup: " + t);
                }
                try
                {
                    ShulkerFolia.tickScan(this, server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in shulker scan: " + t);
                }
                try
                {
                    ParrotFolia.tick(server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in parrot tick: " + t);
                }
                try
                {
                    ProfileFolia.tick(server);
                }
                catch (Throwable t)
                {
                    getLogger().severe("Error in profile tick: " + t);
                }
            }, 1, 1);

            getLogger().info("Carpet Folia enabled");
        } catch (Throwable t) {
            getLogger().severe("Failed to enable FoliaCarpet: " + t);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void registerCarpetCommands() {
        Commands commands = server.getCommands();
        CommandBuildContext context = CommandBuildContext.simple(server.registryAccess(), FeatureFlags.DEFAULT_FLAGS);
        CarpetServer.registerCarpetCommands(commands.getDispatcher(), Commands.CommandSelection.DEDICATED, context);
    }

    private void scheduleDimensionTicks()
    {
        cancelDimensionTicks();
        for (org.bukkit.World world : Bukkit.getWorlds())
        {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = switch (world.getEnvironment())
            {
                case NETHER -> net.minecraft.world.level.Level.NETHER;
                case THE_END -> net.minecraft.world.level.Level.END;
                default -> null;
            };
            if (dimension == null)
            {
                continue;
            }
            dimensionTickTasks.add(Bukkit.getRegionScheduler().runAtFixedRate(
                    this, world, 0, 0,
                    task -> {
                        try
                        {
                            CarpetServer.tickDimensionEvent(server, dimension);
                        }
                        catch (Throwable error)
                        {
                            getLogger().severe("Error in Carpet dimension tick: " + error);
                        }
                    }, 1, 1));
        }
    }

    private void cancelDimensionTicks()
    {
        for (ScheduledTask task : dimensionTickTasks)
        {
            if (task != null)
            {
                task.cancel();
            }
        }
        dimensionTickTasks.clear();
    }

    @Override
    public void onDisable() {
        try {
            cancelDimensionTicks();
            if (server != null) {
                CarpetServer.onServerClosed(null);
            }
            CarpetServer.onServerDoneClosing(server);
            ParrotFolia.detach(this);
            FoliaRuntime.unbind(this);
        } catch (Throwable t) {
            getLogger().severe("Error during FoliaCarpet disable: " + t);
        }
        getLogger().info("Carpet Folia disabled");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
        try {
            CarpetServer.onPlayerLoggedIn(player);
        } catch (Throwable t) {
            getLogger().severe("Error in Carpet onPlayerLoggedIn: " + t);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
        lastQuitPlayer.set(player);
        try {
            CarpetServer.onPlayerLoggedOut(player, Component.literal("player quit"));
        } catch (Throwable t) {
            getLogger().severe("Error in Carpet onPlayerLoggedOut: " + t);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            try {
                CarpetServer.reloadWorlds(server);
                registerCarpetCommands();
                Bukkit.getGlobalRegionScheduler().run(this, task -> scheduleDimensionTicks());
            } catch (Throwable t) {
                getLogger().severe("Error in Carpet onServerLoadedWorlds: " + t);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFailMove(PlayerFailMoveEvent event) {

        Player player = event.getPlayer();
        try {
            if (player.isFlying()
                    && player.getFlySpeed() > 0.1f
                    && (event.getFailReason() == PlayerFailMoveEvent.FailReason.MOVED_TOO_QUICKLY
                    || event.getFailReason() == PlayerFailMoveEvent.FailReason.MOVED_INTO_UNLOADED_CHUNK))
            {
                event.setAllowed(true);
                event.setLogWarning(false);
            }
        } catch (Throwable ignored) {
        }
    }

    public static CarpetFoliaPlugin get() {
        return instance;
    }
}
