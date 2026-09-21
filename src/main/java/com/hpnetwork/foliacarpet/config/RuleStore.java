package com.hpnetwork.foliacarpet.config;

import com.hpnetwork.foliacarpet.FoliaCarpetPlugin;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.configuration.file.FileConfiguration;

public final class RuleStore {
    private final FoliaCarpetPlugin plugin;
    private final Map<String, Rule<?>> rules = new LinkedHashMap<>();
    private final AtomicLong redstoneWindow = new AtomicLong();

    public RuleStore(FoliaCarpetPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        register(new Rule<>("entity-spawn-cap", Integer.class, config.getInt("rules.entity-spawn-cap", 64)));
        register(new Rule<>("redstone-events-per-second", Integer.class,
                config.getInt("rules.redstone-events-per-second", 250)));
        register(new Rule<>("max-view-distance", Integer.class, config.getInt("rules.max-view-distance", 10)));
        register(new Rule<>("max-simulation-distance", Integer.class,
                config.getInt("rules.max-simulation-distance", 8)));
        register(new Rule<>("language", String.class, config.getString("language", "zh-CN")));
    }

    private <T> void register(Rule<T> rule) {
        rules.put(rule.name(), rule);
    }

    public Map<String, Rule<?>> all() {
        return Collections.unmodifiableMap(rules);
    }

    public Rule<?> get(String name) {
        return rules.get(name);
    }

    public boolean set(String name, String raw) {
        Rule<?> rule = get(name);
        if (rule == null || !rule.parseAndSet(raw)) {
            return false;
        }
        String path = name.equals("language") ? "language" : "rules." + name;
        plugin.getConfig().set(path, rule.value());
        plugin.saveConfig();
        if (name.equals("language")) {
            plugin.messages().reload();
        }
        return true;
    }

    public int intValue(String name) {
        Rule<?> rule = get(name);
        return rule != null && rule.value() instanceof Integer value ? value : 0;
    }

    public String stringValue(String name) {
        Rule<?> rule = get(name);
        return rule != null && rule.value() instanceof String value ? value : "";
    }

    public long nextRedstoneWindow() {
        return redstoneWindow.incrementAndGet();
    }

    public static final class Rule<T> {
        private final String name;
        private final Class<T> type;
        private T value;

        public Rule(String name, Class<T> type, T value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public T value() {
            return value;
        }

        private static final int MIN_CAP = 1;
        private static final int MAX_CAP = 10000;

        public boolean parseAndSet(String raw) {
            try {
                T parsed;
                if (type == Integer.class) {
                    int value = Integer.parseInt(raw);
                    if (name.contains("distance")) {
                        if (value < 2 || value > 32) return false;
                    } else if (value < MIN_CAP || value > MAX_CAP) {
                        return false;
                    }
                    parsed = type.cast(value);
                } else {
                    parsed = type.cast(raw);
                    if (name.equals("language") && !raw.equals("zh-CN") && !raw.equals("en-US")) {
                        return false;
                    }
                }
                value = parsed;
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
    }
}
