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

    private final Path filePath;
    private final Map<String, CounterData> counters = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean dirty = false;

    private final Logger log = LoggerFactory.getLogger(JsonCounterManager.class);

    public JsonCounterManager(String fileName, int saveIntervalSeconds) {
        this.filePath = Paths.get(fileName);
        loadCounters();
        scheduleAutoSave(saveIntervalSeconds);
        Runtime.getRuntime().addShutdownHook(new Thread(this::forceSave));
    }

    private void loadCounters() {
        try {
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                Type type = new TypeToken<Map<String, CounterData>>() {}.getType();
                counters.putAll(gson.fromJson(json, type));
            }
        } catch (IOException e) {
            log.error("Failed to save counters: " + e.getMessage());
        }
    }

    private void scheduleAutoSave(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            if (dirty) {
                save();
                dirty = false;
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void increment(String key) {
        adjust(key, 1);
    }
    
    public void adjust(String key, int amount) {
        CounterData counter = get(key);
        if (counter != null) {
            counter.adjust(amount);
        } else {
            log.error("Counter '" + key + "' does not exist.");
        }
        dirty = true;
    }

    public CounterData get(String key) {
        return counters.getOrDefault(key, new CounterData());
    }

    public void set(String key, int value) {
        counters.get(key).value = value;
        dirty = true;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            gson.toJson(counters, writer);
            log.info("Saved to " + filePath);
        } catch (IOException e) {
            log.error("Failed to save counters: " + e.getMessage());
        }
    }

    public void forceSave() {
        save();
        scheduler.shutdown();
    }
}
