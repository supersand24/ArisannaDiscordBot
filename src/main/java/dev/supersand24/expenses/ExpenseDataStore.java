package dev.supersand24.expenses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpenseDataStore {

    private long nextId = 1;

    private Map<Long, ExpenseData> expenses = new ConcurrentHashMap<>();

    public Map<Long, ExpenseData> getExpenses() {
        return expenses;
    }

    public long getAndIncrementNextId() {
        synchronized (this) {
            return nextId++;
        }
    }

}
