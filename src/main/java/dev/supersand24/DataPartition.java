package dev.supersand24;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataPartition<T extends Identifiable> {

    private long nextId = 1;

    private Map<Long, T> data = new ConcurrentHashMap<>();

    /**
     * Returns the map containing the data.
     * A manager class will use this to read and modify the data.
     * @return The map of data objects.
     */
    public Map<Long, T> getData() {
        return data;
    }

    /**
     * Atomically gets the next available ID for a new item and increments the
     * internal counter for the next use. The 'synchronized' keyword ensures this
     * method is thread-safe.
     *
     * @return A new, unique ID.
     */
    public synchronized long getAndIncrementId() {
        return nextId++;
    }

    /**
     * A method that can be called after this object is loaded from JSON.
     * It iterates through its own data and sets the transient ID on each child object.
     * This makes the partition responsible for its own data integrity.
     */
    public void performPostLoadActions() {
        if (data != null) {
            data.forEach((id, item) -> item.setId(id));
        }
    }
}
