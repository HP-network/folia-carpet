package carpet.folia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import carpet.CarpetSettings;

public final class ParrotFolia implements Listener
{
    private static final int PRUNE_TICKS = 20;

    private static Plugin plugin;
    private static final Map<UUID, CompoundTag[]> shoulderSnapshot = new ConcurrentHashMap<>();
    private static final Map<ServerPlayer, DamageInfo> damageTicks = new ConcurrentHashMap<>();

    private static final class DamageInfo
    {
        final long tick;
        final float damage;

        DamageInfo(long tick, float damage)
        {
            this.tick = tick;
            this.damage = damage;
        }
    }

    public ParrotFolia()
    {
    }

    public static void attach(Plugin owner)
    {
        plugin = owner;
    }

    public static void detach(Plugin owner)
    {
        if (plugin == owner)
        {
            plugin = null;
            shoulderSnapshot.clear();
            damageTicks.clear();
        }
    }

    public static void tick(MinecraftServer server)
    {
        if (server == null || !CarpetSettings.persistentParrots)
        {
            return;
        }
        if (plugin == null)
        {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            FoliaRuntime.runOnPlayer(player, target -> {
                CompoundTag left = target.getShoulderEntityLeft();
                CompoundTag right = target.getShoulderEntityRight();
                if (left.isEmpty() && right.isEmpty())
                {
                    shoulderSnapshot.remove(target.getUUID());
                }
                else
                {
                    shoulderSnapshot.put(target.getUUID(), new CompoundTag[]
                            { left.isEmpty() ? null : left.copy(), right.isEmpty() ? null : right.copy() });
                }
            });
        }
        if (!damageTicks.isEmpty())
        {
            long now = FoliaRuntime.tick();
            damageTicks.entrySet().removeIf(e -> now - e.getValue().tick > PRUNE_TICKS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event)
    {
        if (!(event.getEntity() instanceof Player) || !CarpetSettings.persistentParrots)
        {
            return;
        }
        try
        {
            ServerPlayer player = ((CraftPlayer) event.getEntity()).getHandle();
            damageTicks.put(player, new DamageInfo(Bukkit.getCurrentTick(), (float) event.getFinalDamage()));
        }
        catch (Throwable ignored)
        {
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onParrotSpawn(EntitySpawnEvent event)
    {
        if (!CarpetSettings.persistentParrots || !(event.getEntity() instanceof org.bukkit.entity.Parrot))
        {
            return;
        }
        try
        {
            UUID spawnedUuid = event.getEntity().getUniqueId();

            UUID ownerId = null;
            boolean isLeft = false;
            CompoundTag[] ownerTags = null;
            Map<UUID, CompoundTag[]> snapshot = shoulderSnapshot;
            for (Map.Entry<UUID, CompoundTag[]> entry : snapshot.entrySet())
            {
                CompoundTag[] tags = entry.getValue();
                boolean matchLeft = tags[0] != null
                        && spawnedUuid.toString().equals(tags[0].getString("UUID"));
                boolean matchRight = tags[1] != null
                        && spawnedUuid.toString().equals(tags[1].getString("UUID"));
                if (matchLeft || matchRight)
                {
                    ownerId = entry.getKey();
                    isLeft = matchLeft;
                    ownerTags = tags;
                    break;
                }
            }
            if (ownerId == null || plugin == null)
            {
                return;
            }
            Player bukkitPlayer = Bukkit.getPlayer(ownerId);
            if (bukkitPlayer == null || !bukkitPlayer.isOnline())
            {
                return;
            }
            ServerPlayer player = ((CraftPlayer) bukkitPlayer).getHandle();

            AtomicBoolean keep = new AtomicBoolean();
            if (!FoliaRuntime.runOnPlayerAndWait(player, target -> {
                DamageInfo dmg = damageTicks.get(target);
                boolean damagePath = dmg != null && dmg.tick == Bukkit.getCurrentTick();
                if (damagePath)
                {
                    keep.set(target.isShiftKeyDown()
                            || !(target.getRandom().nextFloat() < dmg.damage / 15.0F));
                }
                else
                {
                    boolean carpetRemoves = (target.getAbilities().invulnerable && target.fallDistance > 0.5F)
                            || target.isInWater() || target.getAbilities().flying
                            || target.isSleeping() || target.isInPowderSnow;
                    keep.set(!carpetRemoves);
                }
            }) || !keep.get())
            {
                return;
            }

            event.setCancelled(true);
            final CompoundTag restoreTag = isLeft ? ownerTags[0] : ownerTags[1];
            scheduleRestore(event, player, isLeft, restoreTag);
        }
        catch (Throwable ignored)
        {
        }
    }

    private static void scheduleRestore(EntitySpawnEvent event, ServerPlayer owner, boolean left, CompoundTag tag)
    {
        if (tag == null || plugin == null)
        {
            return;
        }
        FoliaRuntime.runOnPlayer(owner, target -> {
            try
            {

                if (left)
                {
                    if (target.getShoulderEntityLeft().isEmpty())
                    {
                        target.setShoulderEntityLeft(tag);
                    }
                }
                else if (target.getShoulderEntityRight().isEmpty())
                {
                    target.setShoulderEntityRight(tag);
                }
            }
            catch (Throwable ignored)
            {
            }
        });
    }
}
