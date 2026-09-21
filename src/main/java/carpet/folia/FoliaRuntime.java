package carpet.folia;

import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** State shared by the Paper plugin loader and transformed server classes. */
public final class FoliaRuntime
{
    private static final AtomicInteger TICK = new AtomicInteger();
    private static volatile Plugin plugin;

    private FoliaRuntime()
    {
    }

    public static void bind(Plugin value)
    {
        plugin = value;
    }

    public static void unbind(Plugin value)
    {
        if (plugin == value)
        {
            plugin = null;
        }
    }

    public static Plugin plugin()
    {
        Plugin value = plugin;
        if (value != null)
        {
            return value;
        }
        try
        {
            // Mixin support classes are loaded by the server classloader, so
            // their copy of this holder cannot see the plugin-loader field.
            return Bukkit.getPluginManager().getPlugin("FoliaCarpet");
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    public static int tick()
    {
        try
        {
            return Math.toIntExact(io.papermc.paper.threadedregions.RegionizedServer.getCurrentTick());
        }
        catch (Throwable ignored)
        {
            return TICK.get();
        }
    }

    public static int advanceTick()
    {
        return TICK.incrementAndGet();
    }

    /** Runs player-owned work on the entity's Folia region thread. */
    public static void runOnPlayer(ServerPlayer player, Consumer<ServerPlayer> action)
    {
        if (player == null || action == null)
        {
            return;
        }
        try
        {
            if (player instanceof carpet.patches.EntityPlayerMPFake fake)
            {
                runOnFakePlayer(fake, action);
                return;
            }
            org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
            Plugin owner = plugin();
            if (bukkitPlayer == null || owner == null)
            {
                return;
            }
            if (Bukkit.isOwnedByCurrentRegion(bukkitPlayer))
            {
                action.accept(player);
                return;
            }
            bukkitPlayer.getScheduler().execute(owner,
                    () -> action.accept(player),
                    () -> {},
                    1L);
        }
        catch (Throwable ignored)
        {
            // The player may have disconnected between lookup and scheduling.
        }
    }

    /** Sends a system message on the player's region thread. */
    public static void sendSystemMessage(ServerPlayer player, Component message)
    {
        if (message != null)
        {
            runOnPlayer(player, target -> target.sendSystemMessage(message));
        }
    }

    /** Sends a sequence of system messages without losing their order. */
    public static void sendSystemMessages(ServerPlayer player, Collection<? extends Component> messages)
    {
        if (messages == null || messages.isEmpty())
        {
            return;
        }
        java.util.List<Component> copy = java.util.List.copyOf(messages);
        runOnPlayer(player, target -> copy.forEach(target::sendSystemMessage));
    }

    /** Sends a packet from the player's region thread. */
    public static void sendPacket(ServerPlayer player, Packet<?> packet)
    {
        if (packet != null)
        {
            runOnPlayer(player, target -> target.connection.send(packet));
        }
    }

    /** Sends several packets in order from one region task. */
    public static void sendPackets(ServerPlayer player, Packet<?>... packets)
    {
        if (packets == null || packets.length == 0)
        {
            return;
        }
        runOnPlayer(player, target -> {
            for (Packet<?> packet : packets)
            {
                if (packet != null)
                {
                    target.connection.send(packet);
                }
            }
        });
    }

    /** Runs work that belongs to Folia's global region. */
    public static void runOnGlobal(Runnable action)
    {
        if (action == null)
        {
            return;
        }
        Plugin owner = plugin();
        if (owner == null)
        {
            return;
        }
        try
        {
            if (Bukkit.isGlobalTickThread())
            {
                action.run();
            }
            else
            {
                Bukkit.getGlobalRegionScheduler().execute(owner, action);
            }
        }
        catch (Throwable ignored)
        {
            // The server can be between startup and shutdown here.
        }
    }

    /** Delivers a server-wide system message on the global region. */
    public static void sendServerMessage(MinecraftServer server, Component message)
    {
        if (server != null && message != null)
        {
            if (plugin() == null)
            {
                server.sendSystemMessage(message);
            }
            else
            {
                runOnGlobal(() -> server.sendSystemMessage(message));
            }
        }
    }

    /** Preserves command feedback semantics while moving player output to its region. */
    public static void sendCommandSuccess(CommandSourceStack source, Supplier<Component> message,
                                          boolean broadcastToOps)
    {
        if (source == null || message == null)
        {
            return;
        }
        if (source instanceof carpet.script.utils.SnoopyCommandSource)
        {
            source.sendSuccess(message, broadcastToOps);
            return;
        }
        if (source.getEntity() instanceof ServerPlayer player)
        {
            runOnPlayer(player, ignored -> source.sendSuccess(message, broadcastToOps));
        }
        else
        {
            source.sendSuccess(message, broadcastToOps);
        }
    }

    /** Preserves command failure semantics while moving player output to its region. */
    public static void sendCommandFailure(CommandSourceStack source, Component message)
    {
        if (source == null || message == null)
        {
            return;
        }
        if (source instanceof carpet.script.utils.SnoopyCommandSource)
        {
            source.sendFailure(message);
            return;
        }
        if (source.getEntity() instanceof ServerPlayer player)
        {
            runOnPlayer(player, ignored -> source.sendFailure(message));
        }
        else
        {
            source.sendFailure(message);
        }
    }

    /** Runs player-owned work and waits when the caller is on another region. */
    public static boolean runOnPlayerAndWait(ServerPlayer player, Consumer<ServerPlayer> action)
    {
        if (player == null || action == null)
        {
            return false;
        }
        try
        {
            if (player instanceof carpet.patches.EntityPlayerMPFake fake)
            {
                return runOnFakePlayerAndWait(fake, action);
            }
            org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
            Plugin owner = plugin();
            if (bukkitPlayer == null || owner == null)
            {
                return false;
            }
            if (Bukkit.isOwnedByCurrentRegion(bukkitPlayer))
            {
                action.accept(player);
                return true;
            }
            CompletableFuture<Boolean> completed = new CompletableFuture<>();
            bukkitPlayer.getScheduler().execute(owner,
                    () -> {
                        try
                        {
                            action.accept(player);
                            completed.complete(true);
                        }
                        catch (Throwable error)
                        {
                            completed.completeExceptionally(error);
                        }
                    },
                    () -> completed.complete(false),
                    1L);
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static void runOnFakePlayer(carpet.patches.EntityPlayerMPFake player,
                                        Consumer<ServerPlayer> action)
    {
        Plugin owner = plugin();
        org.bukkit.World world = player.level().getWorld();
        if (owner == null || world == null)
        {
            return;
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        if (ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(player.level(), player.blockPosition()))
        {
            action.accept(player);
            return;
        }
        Bukkit.getRegionScheduler().execute(owner, world, chunk.x, chunk.z,
                () -> action.accept(player));
    }

    private static boolean runOnFakePlayerAndWait(carpet.patches.EntityPlayerMPFake player,
                                                   Consumer<ServerPlayer> action)
    {
        Plugin owner = plugin();
        org.bukkit.World world = player.level().getWorld();
        if (owner == null || world == null)
        {
            return false;
        }
        if (ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(player.level(), player.blockPosition()))
        {
            action.accept(player);
            return true;
        }
        CompletableFuture<Boolean> completed = new CompletableFuture<>();
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        Bukkit.getRegionScheduler().execute(owner, world, chunk.x, chunk.z, () -> {
            try
            {
                action.accept(player);
                completed.complete(true);
            }
            catch (Throwable error)
            {
                completed.completeExceptionally(error);
            }
        });
        try
        {
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }
}
