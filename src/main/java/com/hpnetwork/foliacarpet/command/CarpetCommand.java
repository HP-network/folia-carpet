package com.hpnetwork.foliacarpet.command;

import com.hpnetwork.foliacarpet.FoliaCarpetPlugin;
import com.hpnetwork.foliacarpet.config.RuleStore;
import com.hpnetwork.foliacarpet.i18n.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class CarpetCommand implements CommandExecutor, TabCompleter {
    private final FoliaCarpetPlugin plugin;
    private final RuleStore rules;
    private final Messages messages;

    public CarpetCommand(FoliaCarpetPlugin plugin, RuleStore rules, Messages messages) {
        this.plugin = plugin;
        this.rules = rules;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("foliacarpet.command")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(messages.text("rules.header"));
            rules.all().values().forEach(rule -> sender.sendMessage("- " + rule.name() + " = " + rule.value()));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length != 3 || !rules.set(args[1], args[2])) {
                    sender.sendMessage(ChatColor.RED + messages.text("rule.invalid"));
                } else {
                    sender.sendMessage(messages.text("rule.set", args[1], args[2]));
                }
            }
            case "lang" -> {
                if (args.length != 2 || !rules.set("language", args[1])) {
                    sender.sendMessage(ChatColor.RED + messages.text("rule.invalid"));
                } else {
                    sender.sendMessage(messages.text("language.set", args[1]));
                }
            }
            case "status" -> plugin.getServer().getGlobalRegionScheduler().run(plugin,
                    task -> plugin.getServer().getOnlinePlayers().stream().findFirst().ifPresentOrElse(
                            player -> sender.sendMessage(messages.text("status.tps", plugin.getServer().getTPS()[0],
                                    plugin.getServer().getAverageTickTime(), plugin.getServer().getOnlinePlayers().size())),
                            () -> sender.sendMessage(messages.text("status.header"))));
            default -> sender.sendMessage(messages.text("usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("list", "set", "lang", "status");
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return new ArrayList<>(rules.all().keySet());
        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) return List.of("zh-CN", "en-US");
        return List.of();
    }
}
