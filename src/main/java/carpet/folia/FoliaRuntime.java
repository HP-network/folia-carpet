package carpet.folia;

import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;

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

    /** Returns whether an entity may be accessed by the current Folia thread. */
    public static boolean isOwnedByCurrentRegion(Entity entity)
    {
        if (entity == null)
        {
            return false;
        }
        try
        {
            if (entity instanceof ServerPlayer player)
            {
                org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
                return bukkitPlayer != null && Bukkit.isOwnedByCurrentRegion(bukkitPlayer);
            }
            return entity.level() instanceof ServerLevel level
                    && isCurrentRegion(level, entity.blockPosition());
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /** Returns whether the current thread is Folia's global scheduler thread. */
    public static boolean isGlobalThread()
    {
        try
        {
            return Bukkit.isGlobalTickThread()
                    || io.papermc.paper.threadedregions.RegionizedServer.isGlobalTickThread();
        }
        catch (Throwable ignored)
        {
            try
            {
                return io.papermc.paper.threadedregions.RegionizedServer.isGlobalTickThread();
            }
            catch (Throwable ignoredAgain)
            {
                return false;
            }
        }
    }

    private static boolean isCurrentRegion(ServerLevel level, BlockPos pos)
    {
        if (level == null || pos == null)
        {
            return false;
        }
        try
        {
            io.papermc.paper.threadedregions.RegionizedWorldData current =
                    io.papermc.paper.threadedregions.TickRegionScheduler.getCurrentRegionizedWorldData();
            return current != null && current.world == level
                    && ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(level, pos);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /** Runs work for a world position on the region that owns that position. */
    public static void runOnRegion(ServerLevel level, BlockPos pos, Runnable action)
    {
        if (level == null || pos == null || action == null)
        {
            return;
        }
        Plugin owner = plugin();
        org.bukkit.World world = level.getWorld();
        if (owner == null || world == null)
        {
            return;
        }
        try
        {
            ChunkPos chunk = new ChunkPos(pos);
            if (isCurrentRegion(level, pos))
            {
                action.run();
                return;
            }
            scheduleRegionTask(owner, world, chunk, action, () -> {});
        }
        catch (Throwable ignored)
        {
            // A region can retire while a command is being dispatched.
        }
    }

    /** Runs world-position work synchronously unless called from the global region. */
    public static boolean runOnRegionAndWait(ServerLevel level, BlockPos pos, Runnable action)
    {
        if (level == null || pos == null || action == null)
        {
            return false;
        }
        Plugin owner = plugin();
        org.bukkit.World world = level.getWorld();
        if (owner == null || world == null)
        {
            return false;
        }
        try
        {
            ChunkPos chunk = new ChunkPos(pos);
            if (isCurrentRegion(level, pos))
            {
                action.run();
                return true;
            }
            if (isGlobalThread())
            {
                runOnRegion(level, pos, action);
                return false;
            }
            CompletableFuture<Boolean> completed = new CompletableFuture<>();
            scheduleRegionTask(owner, world, chunk, () ->
            {
                try
                {
                    action.run();
                    completed.complete(true);
                }
                catch (Throwable error)
                {
                    completed.completeExceptionally(error);
                }
            }, () -> completed.complete(false));
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /** Reads or mutates world state on its owning region and returns a fallback on async dispatch. */
    public static <T> T callOnRegionAndWait(ServerLevel level, BlockPos pos,
                                            Supplier<T> action, T fallback)
    {
        if (level == null || pos == null || action == null)
        {
            return fallback;
        }
        Plugin owner = plugin();
        org.bukkit.World world = level.getWorld();
        if (owner == null || world == null)
        {
            return fallback;
        }
        try
        {
            ChunkPos chunk = new ChunkPos(pos);
            if (isCurrentRegion(level, pos))
            {
                return action.get();
            }
            if (isGlobalThread())
            {
                runOnRegion(level, pos, action::get);
                return fallback;
            }
            CompletableFuture<T> completed = new CompletableFuture<>();
            scheduleRegionTask(owner, world, chunk, () ->
            {
                try
                {
                    completed.complete(action.get());
                }
                catch (Throwable error)
                {
                    completed.completeExceptionally(error);
                }
            }, () -> completed.complete(fallback));
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return fallback;
        }
    }

    private static void scheduleRegionTask(Plugin owner, org.bukkit.World world, ChunkPos chunk,
                                           Runnable action, Runnable failure)
    {
        try
        {
            if (world.isChunkLoaded(chunk.x, chunk.z))
            {
                Bukkit.getRegionScheduler().execute(owner, world, chunk.x, chunk.z, action);
                return;
            }
            world.getChunkAtAsync(chunk.x, chunk.z, true, false, chunkResult ->
            {
                if (chunkResult == null)
                {
                    failure.run();
                    return;
                }
                try
                {
                    Bukkit.getRegionScheduler().execute(owner, world, chunk.x, chunk.z, action);
                }
                catch (Throwable error)
                {
                    failure.run();
                }
            });
        }
        catch (Throwable error)
        {
            failure.run();
        }
    }

    /** Returns the server's entity index without restricting the result to the current region. */
    public static List<Entity> allEntities(ServerLevel level)
    {
        if (level == null)
        {
            return List.of();
        }
        try
        {
            if (level.getEntities() instanceof ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup lookup)
            {
                List<Entity> result = new ArrayList<>();
                lookup.getAllMapped().forEach(entity -> result.add((Entity) entity));
                return result;
            }
        }
        catch (Throwable ignored)
        {
        }
        try
        {
            List<Entity> result = new ArrayList<>();
            level.getAllEntities().forEach(result::add);
            return result;
        }
        catch (Throwable ignored)
        {
            return List.of();
        }
    }

    /** Runs entity-owned work on the entity's current Folia region. */
    public static void runOnEntity(Entity entity, Consumer<Entity> action)
    {
        if (entity == null || action == null)
        {
            return;
        }
        if (entity instanceof ServerPlayer player)
        {
            runOnPlayer(player, action::accept);
            return;
        }
        Plugin owner = plugin();
        if (owner == null)
        {
            action.accept(entity);
            return;
        }
        try
        {
            if (isOwnedByCurrentRegion(entity))
            {
                action.accept(entity);
                return;
            }
            if (!entity.getBukkitEntity().getScheduler().execute(owner,
                    () -> action.accept(entity), () -> {}, 1L))
            {
                return;
            }
        }
        catch (Throwable ignored)
        {
            // Entities can be removed or unload while a task is being queued.
        }
    }

    /** Runs entity-owned work synchronously when the caller needs vanilla ordering. */
    public static boolean runOnEntityAndWait(Entity entity, Consumer<Entity> action)
    {
        if (entity == null || action == null)
        {
            return false;
        }
        if (entity instanceof ServerPlayer player)
        {
            return runOnPlayerAndWait(player, action::accept);
        }
        Plugin owner = plugin();
        if (owner == null || isOwnedByCurrentRegion(entity))
        {
            try
            {
                action.accept(entity);
                return true;
            }
            catch (Throwable ignored)
            {
                return false;
            }
        }
        try
        {
            CompletableFuture<Boolean> completed = new CompletableFuture<>();
            if (!entity.getBukkitEntity().getScheduler().execute(owner,
                    () -> {
                        try
                        {
                            action.accept(entity);
                            completed.complete(true);
                        }
                        catch (Throwable error)
                        {
                            completed.completeExceptionally(error);
                        }
                    }, () -> completed.complete(false), 1L))
            {
                return false;
            }
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /** Reads entity-owned state on its region and returns it to the caller. */
    public static <T> T callOnEntityAndWait(Entity entity, Function<Entity, T> action, T fallback)
    {
        if (entity == null || action == null)
        {
            return fallback;
        }
        // Offline/fake players are owned by their region, but their state is
        // also intentionally exposed to global Carpet commands. Reads do not
        // enqueue work and, importantly, never block the global region.
        if (entity instanceof carpet.patches.EntityPlayerMPFake && isGlobalThread())
        {
            try
            {
                return action.apply(entity);
            }
            catch (Throwable ignored)
            {
                return fallback;
            }
        }
        if (entity instanceof ServerPlayer player)
        {
            try
            {
                CompletableFuture<T> result = new CompletableFuture<>();
                if (!runOnPlayerAndWait(player, target -> result.complete(action.apply(target))))
                {
                    return fallback;
                }
                return result.getNow(fallback);
            }
            catch (Throwable ignored)
            {
                return fallback;
            }
        }
        Plugin owner = plugin();
        if (owner == null || isOwnedByCurrentRegion(entity))
        {
            try
            {
                return action.apply(entity);
            }
            catch (Throwable ignored)
            {
                return fallback;
            }
        }
        try
        {
            CompletableFuture<T> completed = new CompletableFuture<>();
            if (!entity.getBukkitEntity().getScheduler().execute(owner,
                    () -> {
                        try
                        {
                            completed.complete(action.apply(entity));
                        }
                        catch (Throwable error)
                        {
                            completed.completeExceptionally(error);
                        }
                    }, () -> completed.completeExceptionally(new IllegalStateException("Entity scheduler retired")), 1L))
            {
                return fallback;
            }
            return completed.get(5, TimeUnit.SECONDS);
        }
        catch (Throwable ignored)
        {
            return fallback;
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
            if (isGlobalThread())
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
            // The global region must never wait for a region task: Folia needs
            // the global thread to keep driving the region scheduler.
            if (isGlobalThread())
            {
                runOnPlayer(player, action);
                return false;
            }
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
        if (isGlobalThread())
        {
            runOnFakePlayer(player, action);
            return false;
        }
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
