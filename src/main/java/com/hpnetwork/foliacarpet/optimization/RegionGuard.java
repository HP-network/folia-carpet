package com.hpnetwork.foliacarpet.optimization;

import com.hpnetwork.foliacarpet.FoliaCarpetPlugin;
import com.hpnetwork.foliacarpet.config.RuleStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class RegionGuard implements Listener {
    private final RuleStore rules;
    private final Map<UUID, AtomicInteger> spawned = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> redstone = new ConcurrentHashMap<>();

    public RegionGuard(FoliaCarpetPlugin plugin, RuleStore rules) {
        this.rules = rules;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        UUID world = event.getLocation().getWorld().getUID();
        AtomicInteger count = spawned.computeIfAbsent(world, ignored -> new AtomicInteger());
        if (count.incrementAndGet() > rules.intValue("entity-spawn-cap")) {
            count.decrementAndGet();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        AtomicInteger count = spawned.get(event.getEntity().getWorld().getUID());
        if (count != null) count.updateAndGet(value -> Math.max(0, value - 1));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        UUID world = event.getBlock().getWorld().getUID();
        AtomicLong count = redstone.computeIfAbsent(world, ignored -> new AtomicLong());
        if (count.incrementAndGet() > rules.intValue("redstone-events-per-second")) {
            count.decrementAndGet();
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    public void resetBudgets() {
        redstone.values().forEach(value -> value.set(0));
    }
}
