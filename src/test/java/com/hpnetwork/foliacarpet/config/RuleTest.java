package com.hpnetwork.foliacarpet.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleTest {
    @Test
    void acceptsValuesWithinTheRuleRange() {
        RuleStore.Rule<Integer> rule = new RuleStore.Rule<>("entity-spawn-cap", Integer.class, 64);

        assertTrue(rule.parseAndSet("128"));
        assertEquals(128, rule.value());
    }

    @Test
    void rejectsInvalidDistanceAndLanguageValues() {
        RuleStore.Rule<Integer> distance = new RuleStore.Rule<>("max-view-distance", Integer.class, 10);
        RuleStore.Rule<String> language = new RuleStore.Rule<>("language", String.class, "zh-CN");

        assertFalse(distance.parseAndSet("1"));
        assertEquals(10, distance.value());
        assertFalse(language.parseAndSet("de-DE"));
        assertEquals("zh-CN", language.value());
    }
}
