package dev.supersand24.expenses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.supersand24.counters.CounterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpenseManager {

    private static Map<String, ExpenseData> expenses = new ConcurrentHashMap<>();
    private static final Path filePath = Paths.get("data/expenses.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean dirty = false;

    private static final Logger log = LoggerFactory.getLogger(ExpenseManager.class);

    public static void ready(int saveIntervalSeconds) {
        loadExpenses();
        scheduleAutoSave(saveIntervalSeconds);
        Runtime.getRuntime().addShutdownHook(new Thread(ExpenseManager::forceSave));
    }

    public static void loadExpenses() {
        try {
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                Type type = new TypeToken<Map<String, ExpenseData>>() {}.getType();
                expenses.putAll(gson.fromJson(json, type));
            }
        } catch (IOException e) {
            log.error("Failed to load counters: " + e.getMessage());
        }
    }

    public static void saveExpenses() {
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            gson.toJson(expenses, writer);
            writer.flush();
            log.info("Saved to " + filePath);
        } catch (IOException e) {
            log.error("Failed to save counters: " + e.getMessage());
        }
    }

    private static void scheduleAutoSave(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            if (dirty) {
                saveExpenses();
                dirty = false;
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public static void forceSave() {
        saveExpenses();
        scheduler.shutdown();
    }

    public static String getInteractionPrefix() {
        return "expense-beneficiary-select:";
    }

    public static String createExpense(String name, double amount, String payerId) {
        ExpenseData expense = new ExpenseData(name, amount, payerId);
        expenses.put(expense.expenseId, expense);
        saveExpenses();
        return expense.expenseId;
    }

    public static void deleteExpense(String key) {
        expenses.remove(key);
        dirty = true;
    }

    public static boolean exists(String key) {
        return expenses.containsKey(key);
    }

    public static String getName(String key) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            return expense.name;
        } else {
            log.error("Expense " + key + " does not exist.");
            return "???";
        }
    }

    public static void setAmount(String key, double amount) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            expense.amount = amount;
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static double getAmount(String key) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            return expense.amount;
        } else {
            log.error("Expense " + key + " does not exist.");
            return 0;
        }
    }

    public static boolean isBenefitingFromExpense(String key, String userId) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            return expense.beneficiaryIds.contains(userId);
        } else {
            log.error("Expense " + key + " does not exist.");
            return false;
        }
    }

    public static void addBenefactor(String key, String benefactorId) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            expense.beneficiaryIds.add(benefactorId);
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void addBenefactors(String key, List<String> benefactorIds) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            for (String id : benefactorIds) {
                if (!expense.beneficiaryIds.contains(id)) {
                    expense.beneficiaryIds.add(id);
                    dirty = true;
                }
            }
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void removeBenefactor(String key, String benefactorId) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            expense.beneficiaryIds.remove(benefactorId);
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void removeBenefactors(String key, List<String> benefactorIds) {
        ExpenseData expense = expenses.get(key);
        if (expense != null) {
            for (String id : benefactorIds) {
                if (expense.beneficiaryIds.contains(id)) {
                    expense.beneficiaryIds.remove(id);
                    dirty = true;
                }
            }
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

}
