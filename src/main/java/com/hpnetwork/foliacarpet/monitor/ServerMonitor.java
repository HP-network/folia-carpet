package com.hpnetwork.foliacarpet.monitor;

import com.hpnetwork.foliacarpet.FoliaCarpetPlugin;
import com.hpnetwork.foliacarpet.config.RuleStore;
import com.hpnetwork.foliacarpet.i18n.Messages;
import com.hpnetwork.foliacarpet.optimization.RegionGuard;
import org.bukkit.Bukkit;

public final class ServerMonitor {
    private final FoliaCarpetPlugin plugin;
    private final RuleStore rules;
    private final Messages messages;
    private final RegionGuard guard;

    public ServerMonitor(FoliaCarpetPlugin plugin, RuleStore rules, Messages messages, RegionGuard guard) {
        this.plugin = plugin;
        this.rules = rules;
        this.messages = messages;
        this.guard = guard;
    }

    public void start() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            guard.resetBudgets();
            int viewDistance = rules.intValue("max-view-distance");
            int simulationDistance = rules.intValue("max-simulation-distance");
            Bukkit.getWorlds().forEach(world -> {
                if (world.getViewDistance() > viewDistance) world.setViewDistance(viewDistance);
                if (world.getSimulationDistance() > simulationDistance) world.setSimulationDistance(simulationDistance);
            });
            plugin.getLogger().fine(messages.text("status.tps", plugin.getServer().getTPS()[0],
                    plugin.getServer().getAverageTickTime(), plugin.getServer().getOnlinePlayers().size()));
        }, 20, 20);
    }
}
