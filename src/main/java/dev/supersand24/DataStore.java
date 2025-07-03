package dev.supersand24;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataStore {

    private static final Logger log = LoggerFactory.getLogger(DataStore.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, Partition<?>> partitions = new ConcurrentHashMap<>();

    private static final Path DATA_DIRECTORY = Paths.get("data");

    private static class Partition<T> {
        final Path filePath;
        final Type type;
        volatile boolean dirty = false;
        T data;

        Partition(Path filePath, Type type, Supplier<T> defaultSupplier) {
            this.filePath = filePath;
            this.type = type;
            this.data = defaultSupplier.get();
        }
    }

    /**
     * Initializes the entire data service. This should be called once at bot startup.
     * It loads all data, starts the auto-save timer, and registers the shutdown hook.
     * @param saveIntervalSeconds How often to check for changes to save.
     */
    public static void initialize(int saveIntervalSeconds) {
        log.info("Initializing DataStoreService...");
        // Ensure the main data directory exists before loading anything.
        try {
            Files.createDirectories(DATA_DIRECTORY);
        } catch (IOException e) {
            log.error("Could not create data directory!", e);
        }
        loadAll();
        scheduleAutoSave(saveIntervalSeconds);
        Runtime.getRuntime().addShutdownHook(new Thread(DataStore::shutdown));
        log.info("DataStoreService ready.");
    }

    /**
     * Registers a new data type to be managed by the service.
     * This should be called once at bot startup for each data file.
     *
     * @param name A unique name for this data partition (e.g., "expenses").
     * @param fileName The name of the JSON file (e.g., "expenses.json").
     * @param type The Gson TypeToken for deserialization.
     * @param defaultSupplier A function that provides a new, empty object if the file doesn't exist.
     */
    public static <T> void register(String name, String fileName, Type type, Supplier<T> defaultSupplier) {
        Path filePath = DATA_DIRECTORY.resolve(fileName);
        partitions.put(name, new Partition<>(filePath, type, defaultSupplier));
    }

    /**
     * Marks a data partition as "dirty", indicating it has changed and needs to be saved.
     * This is the method your manager classes should call after modifying data.
     * @param name The name of the partition to mark (e.g., "expenses").
     */
    public static void markDirty(String name) {
        Partition<?> partition = partitions.get(name);
        if (partition != null) {
            partition.dirty = true;
        }
    }

    /**
     * Gets the in-memory data object for a specific partition.
     * @param name The name of the partition to get (e.g., "expenses").
     * @return The data object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String name) {
        Partition<?> partition = partitions.get(name);
        if (partition == null) {
            throw new IllegalArgumentException("No partition registered with name: " + name);
        }
        return (T) partition.data;
    }


    private static void loadAll() {
        log.info("Loading all data partitions...");

        for (Map.Entry<String, Partition<?>> entry : partitions.entrySet()) {
            Partition<?> partition = entry.getValue();
            try {
                Files.createDirectories(partition.filePath.getParent());
                if (Files.exists(partition.filePath)) {
                    String json = Files.readString(partition.filePath);
                    if (json != null && !json.isEmpty()) {

                        partition.data = gson.fromJson(json, partition.type);
                        log.info("Successfully loaded partition '{}' from {}", entry.getKey(), partition.filePath);

                        if (partition.data instanceof DataPartition) {
                            log.info("Detected DataPartition for '{}'. Running post-load actions...", entry.getKey());
                            // If it is, cast it and call the method directly.
                            ((DataPartition<?>) partition.data).performPostLoadActions();
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to load data for partition '{}' from {}", entry.getKey(), partition.filePath, e);
            }
        }
    }

    private static void save(String name) {
        Partition<?> partition = partitions.get(name);
        if (partition == null) return;

        try {
            String json = gson.toJson(partition.data);
            Files.writeString(partition.filePath, json);
            log.info("Saved partition '{}' to {}", name, partition.filePath);
        } catch (IOException e) {
            log.error("Failed to save data for partition '{}' to {}", name, partition.filePath, e);
        }
    }

    private static void scheduleAutoSave(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Partition<?>> entry : partitions.entrySet()) {
                Partition<?> partition = entry.getValue();
                if (partition.dirty) {
                    save(entry.getKey());
                    partition.dirty = false; // Reset the flag after saving
                }
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private static void shutdown() {
        log.info("Shutdown hook triggered. Saving all dirty data partitions...");
        scheduler.shutdown(); // Stop the scheduler from starting new saves
        for (Map.Entry<String, Partition<?>> entry : partitions.entrySet()) {
            if (entry.getValue().dirty) {
                save(entry.getKey()); // Force-save any remaining dirty data
            }
        }
        log.info("Data saving complete. Goodbye.");
    }

}
