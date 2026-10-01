package dev.supersand24.counters;

import java.util.*;

import dev.supersand24.DataStore;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterManager {

    private static final Logger log = LoggerFactory.getLogger(CounterManager.class);

    public static void createCounter(String guildId, String name, String description, int initialValue, int minValue, int maxValue, String userId) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = new CounterData(description, initialValue, minValue, maxValue, userId);
        counters.put(name, counter);
        DataStore.markDirty(guildId, "counters");
    }

    public static void deleteCounter(String guildId, String key) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        counters.remove(key);
        DataStore.markDirty(guildId, "counters");
    }

    public static MessageEmbed getCounterEmbed(String guildId, String key) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        return counters.get(key).toEmbed(key);
    }

    public static void setDescription(String guildId, String key, String description) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        counters.get(key).description = description;
        DataStore.markDirty(guildId, "counters");
    }

    public static void increment(String guildId, String key) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.increment();
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static void decrement(String guildId, String key) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.decrement();
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static void setValue(String guildId, String key, int value) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.set(value);
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static int getValue(String guildId, String key) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.get();
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
            return 0;
        }
    }

    public static void setMinValue(String guildId, String key, int minValue) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.minValue = minValue;
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static void setMaxValue(String guildId, String key, int maxValue) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.maxValue = maxValue;
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static boolean canEdit(String guildId, String key, String userId) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.allowedEditors.contains(userId);
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
            return false;
        }
    }

    public static void addEditor(String guildId, String key, String userId) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.add(userId);
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static void removeEditor(String guildId, String key, String userId) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.remove(userId);
            DataStore.markDirty(guildId, "counters");
        } else {
            log.error("Counter " + key + " does not exist in guild " + guildId);
        }
    }

    public static Set<String> getCounterNames(String guildId) {
        Map<String, CounterData> counters = DataStore.get(guildId, "counters");
        return counters.keySet();
    }
}