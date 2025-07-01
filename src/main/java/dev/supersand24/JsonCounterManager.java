package dev.supersand24;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonCounterManager {

    private static final Path filePath = Paths.get("data/counters.json");
    private static final Map<String, CounterData> counters = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean dirty = false;

    private static final Logger log = LoggerFactory.getLogger(JsonCounterManager.class);

    public static void ready(int saveIntervalSeconds) {
        loadCounters();
        scheduleAutoSave(saveIntervalSeconds);
        Runtime.getRuntime().addShutdownHook(new Thread(JsonCounterManager::forceSave));
    }

    private static void loadCounters() {
        try {
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                Type type = new TypeToken<Map<String, CounterData>>() {}.getType();
                counters.putAll(gson.fromJson(json, type));
            }
        } catch (IOException e) {
            log.error("Failed to load counters: " + e.getMessage());
        }
    }

    private static void scheduleAutoSave(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            if (dirty) {
                save();
                dirty = false;
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public static void increment(String key) {
        adjust(key, 1);
    }
    
    public static void adjust(String key, int amount) {
        CounterData counter = get(key);
        if (counter != null) {
            counter.adjust(amount);
            dirty = true;
        } else {
            log.error("Counter '" + key + "' does not exist.");
        }
    }

    public static String createCounter(String name, String description, int initialValue, int minValue, int maxValue, String userId) {
        CounterData counter = new CounterData(name, description, initialValue, minValue, maxValue, userId);
        counters.put(name, counter);
        dirty = true;
        return name;
    }

    public static void deleteCouner(String key) {
        counters.remove(key);
        dirty = true;
    }

    public static CounterData get(String key) {
        return counters.get(key);
    }

    public static void setDescription(String key, String description) {
        get(key).description = description;
        markDirty();
    }

    public static void setMinValue(String key, int minValue) {
        get(key).minValue = minValue;
        markDirty();
    }

    public static void setMaxValue(String key, int maxValue) {
        get(key).maxValue = maxValue;
        markDirty();
    }

    public static Set<String> getCounterNames() {
        return counters.keySet();
    }

    public static void set(String key, int value) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.value = value;
            dirty = true;
        } else {
            log.error("Counter '" + key + "' does not exist.");
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            gson.toJson(counters, writer);
            writer.flush();
            log.info("Saved to " + filePath);
        } catch (IOException e) {
            log.error("Failed to save counters: " + e.getMessage());
        }
    }

    public static void forceSave() {
        save();
        scheduler.shutdown();
    }
}
