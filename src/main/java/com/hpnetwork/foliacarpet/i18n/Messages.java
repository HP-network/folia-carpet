package com.hpnetwork.foliacarpet.i18n;

import com.hpnetwork.foliacarpet.FoliaCarpetPlugin;
import java.util.HashMap;
import java.util.Map;

public final class Messages {
    private final FoliaCarpetPlugin plugin;
    private final Map<String, String> values = new HashMap<>();

    public Messages(FoliaCarpetPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        values.clear();
        boolean english = plugin.getConfig().getString("language", "zh-CN").equals("en-US");
        if (english) {
            values.put("plugin.enabled", "FoliaCarpet enabled with region-safe controls.");
            values.put("rules.header", "FoliaCarpet rules:");
            values.put("rule.set", "Rule %s is now %s.");
            values.put("rule.invalid", "Unknown rule or invalid value.");
            values.put("language.set", "Language changed to %s.");
            values.put("status.header", "FoliaCarpet status");
            values.put("status.tps", "TPS: %.1f | MSPT: %.1f | Players: %d");
            values.put("usage", "/carpet list | /carpet set <rule> <value> | /carpet lang <zh-CN|en-US> | /carpet status");
        } else {
            values.put("plugin.enabled", "FoliaCarpet 已启用，区域安全控制已加载。");
            values.put("rules.header", "FoliaCarpet 规则：");
            values.put("rule.set", "规则 %s 已设置为 %s。");
            values.put("rule.invalid", "未知规则或值无效。");
            values.put("language.set", "语言已切换为 %s。");
            values.put("status.header", "FoliaCarpet 状态");
            values.put("status.tps", "TPS：%.1f | MSPT：%.1f | 玩家：%d");
            values.put("usage", "/carpet list | /carpet set <规则> <值> | /carpet lang <zh-CN|en-US> | /carpet status");
        }
    }

    public String text(String key, Object... args) {
        return String.format(values.getOrDefault(key, key), args);
    }
}
