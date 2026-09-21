package com.hpnetwork.foliacarpet;

import com.hpnetwork.foliacarpet.command.CarpetCommand;
import com.hpnetwork.foliacarpet.config.RuleStore;
import com.hpnetwork.foliacarpet.i18n.Messages;
import com.hpnetwork.foliacarpet.monitor.ServerMonitor;
import com.hpnetwork.foliacarpet.optimization.RegionGuard;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaCarpetPlugin extends JavaPlugin {
    private RuleStore rules;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rules = new RuleStore(this);
        messages = new Messages(this);

        RegionGuard guard = new RegionGuard(this, rules);
        getServer().getPluginManager().registerEvents(guard, this);

        CarpetCommand command = new CarpetCommand(this, rules, messages);
        getCommand("carpet").setExecutor(command);
        getCommand("carpet").setTabCompleter(command);

        new ServerMonitor(this, rules, messages, guard).start();
        getLogger().info(messages.text("plugin.enabled"));
    }

    public RuleStore rules() {
        return rules;
    }

    public Messages messages() {
        return messages;
    }
}
