package dev.supersand24.counters;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.supersand24.DataStore;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterManager {

    private static final Logger log = LoggerFactory.getLogger(CounterManager.class);

    public static void createCounter(String name, String description, int initialValue, int minValue, int maxValue, String userId) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = new CounterData(name, description, initialValue, minValue, maxValue, userId);
        counters.put(name, counter);
        DataStore.markDirty("counters");
    }

    public static void deleteCounter(String key) {
        Map<String, CounterData> counters = DataStore.get("counters");
        counters.remove(key);
        DataStore.markDirty("counters");
    }

    public static MessageEmbed getCounterEmbed(String key) {
        Map<String, CounterData> counters = DataStore.get("counters");
        return counters.get(key).toEmbed();
    }

    public static void setDescription(String key, String description) {
        Map<String, CounterData> counters = DataStore.get("counters");
        counters.get(key).description = description;
        DataStore.markDirty("counters");
    }

    public static void increment(String key) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.increment();
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void decrement(String key) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.decrement();
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void setValue(String key, int value) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.value = value;
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static int getValue(String key) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.value;
        } else {
            log.error("Counter " + key + " does not exist.");
            return 0;
        }
    }

    public static void setMinValue(String key, int minValue) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.minValue = minValue;
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void setMaxValue(String key, int maxValue) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.maxValue = maxValue;
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static boolean canEdit(String key, String userId) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.allowedEditors.contains(userId);
        } else {
            log.error("Counter " + key + " does not exist.");
            return false;
        }
    }

    public static void addEditor(String key, String userId) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.add(userId);
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void removeEditor(String key, String userId) {
        Map<String, CounterData> counters = DataStore.get("counters");
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.remove(userId);
            DataStore.markDirty("counters");
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static Set<String> getCounterNames() {
        Map<String, CounterData> counters = DataStore.get("counters");
        return counters.keySet();
    }
}
