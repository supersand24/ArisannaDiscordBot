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

import net.dv8tion.jda.api.entities.MessageEmbed;
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

                for (Map.Entry<String, CounterData> entry : counters.entrySet()) {
                    CounterData counterData = entry.getValue();
                    counterData.name = entry.getKey();
                }
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

    public static void createCounter(String name, String description, int initialValue, int minValue, int maxValue, String userId) {
        CounterData counter = new CounterData(name, description, initialValue, minValue, maxValue, userId);
        counters.put(name, counter);
        dirty = true;
    }

    public static void deleteCounter(String key) {
        counters.remove(key);
        dirty = true;
    }

    public static MessageEmbed getCounterEmbed(String key) {
        return counters.get(key).toEmbed();
    }

    public static void setDescription(String key, String description) {
        counters.get(key).description = description;
        dirty = true;
    }

    public static void increment(String key) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.increment();
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void decrement(String key) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.decrement();
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void setValue(String key, int value) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.value = value;
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static int getValue(String key) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.value;
        } else {
            log.error("Counter " + key + " does not exist.");
            return 0;
        }
    }

    public static void setMinValue(String key, int minValue) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.minValue = minValue;
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void setMaxValue(String key, int maxValue) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.maxValue = maxValue;
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static boolean canEdit(String key, String userId) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            return counter.allowedEditors.contains(userId);
        } else {
            log.error("Counter " + key + " does not exist.");
            return false;
        }
    }

    public static void addEditor(String key, String userId) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.add(userId);
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static void removeEditor(String key, String userId) {
        CounterData counter = counters.get(key);
        if (counter != null) {
            counter.allowedEditors.remove(userId);
            dirty = true;
        } else {
            log.error("Counter " + key + " does not exist.");
        }
    }

    public static Set<String> getCounterNames() {
        return counters.keySet();
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
