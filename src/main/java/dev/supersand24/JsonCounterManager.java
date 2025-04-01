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
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();
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
                Type type = new TypeToken<Map<String, Integer>>() {}.getType();
                Map<String, Integer> raw = gson.fromJson(json, type);
                for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                    counters.put(entry.getKey(), entry.getValue().intValue());
                }
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
        counters.merge(key, amount, Integer::sum);
        dirty = true;
    }

    public int get(String key) {
        return counters.getOrDefault(key, 0);
    }

    public void set(String key, int value) {
        counters.put(key, value);
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
