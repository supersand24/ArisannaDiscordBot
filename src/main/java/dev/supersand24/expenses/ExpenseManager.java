package dev.supersand24.expenses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.supersand24.CurrencyUtils;
import dev.supersand24.Paginator;
import dev.supersand24.counters.CounterData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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

                for (Map.Entry<String, ExpenseData> entry : expenses.entrySet()) {
                    ExpenseData expenseData = entry.getValue();
                    expenseData.expenseId = entry.getKey();
                }
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

    public static Collection<ExpenseData> getExpenses() {
        return expenses.values();
    }

    public static List<ExpenseData> getExpensesSorted() {
        return expenses.values().stream()
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static List<ExpenseData> getExpensesForUserSorted(String userId) {
        return expenses.values().stream()
                .filter(exp -> exp.payerId.equals(userId) || exp.beneficiaryIds.contains(userId))
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static List<String> calculateSettlement() {
        Map<String, Double> balances = new HashMap<>();

        if (expenses.isEmpty()) {
            return Collections.singletonList("There are no expenses to settle.");
        }

        for (ExpenseData expense : expenses.values()) {
            String payerId = expense.payerId;
            double totalAmount = expense.amount;
            List<String> beneficiaries = expense.beneficiaryIds;

            // Skip expenses with no beneficiaries
            if (beneficiaries == null || beneficiaries.isEmpty()) continue;

            balances.put(payerId, balances.getOrDefault(payerId, 0.0) + totalAmount);

            double share = totalAmount / beneficiaries.size();
            for (String beneficiaryId : beneficiaries) {
                balances.put(beneficiaryId, balances.getOrDefault(beneficiaryId, 0.0) - share);
            }
        }

        List<Map.Entry<String, Double>> creditors = new ArrayList<>();
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            if (entry.getValue() > 0.01) {
                creditors.add(entry);
            } else if (entry.getValue() < -0.01) {
                debtors.add(entry);
            }
        }

        if (debtors.isEmpty() && creditors.isEmpty()) {
            return Collections.singletonList("Everyone is already settled up!");
        }

        // This algorithm minimizes the number of payments.
        List<String> transactions = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Map.Entry<String, Double> debtorEntry = debtors.getFirst();
            Map.Entry<String, Double> creditorEntry = creditors.getFirst();

            String debtorId = debtorEntry.getKey();
            String creditorId = creditorEntry.getKey();

            double debtorOwes = Math.abs(debtorEntry.getValue());
            double creditorIsOwed = creditorEntry.getValue();

            // The amount to transfer is the smaller of the two amounts.
            double transferAmount = Math.min(debtorOwes, creditorIsOwed);

            // Create the transaction string.
            transactions.add(
                    String.format("<@%s> owes <@%s> **%s**",
                            debtorId,
                            creditorId,
                            CurrencyUtils.formatAsUSD(transferAmount)
                    )
            );

            // Update the balances in our temporary lists.
            debtorEntry.setValue(debtorEntry.getValue() + transferAmount);
            creditorEntry.setValue(creditorEntry.getValue() - transferAmount);

            // If a debtor has paid off their entire debt, remove them from the list.
            if (Math.abs(debtorEntry.getValue()) < 0.01) {
                debtors.removeFirst();
            }

            // If a creditor has been paid all they are owed, remove them.
            if (creditorEntry.getValue() < 0.01) {
                creditors.removeFirst();
            }
        }
        return transactions;
    }

    public static MessageCreateData buildExpenseListPage(int page, String authorId, String targetId) {
        List<ExpenseData> expenses = targetId.equals("all")
                ? getExpensesSorted()
                : getExpensesForUserSorted(targetId);

        if (expenses.isEmpty())
            return new MessageCreateBuilder().setContent("No expenses found matching criteria.").build();

        Paginator<ExpenseData> paginator = new Paginator<>(expenses);
        final int itemsPerPage = 5;

        Function<ExpenseData, String> formatter = (expense) -> String.format(
                "**%s** - %s\nPaid by <@%s> | ID: `%s`\n\n",
                expense.name,
                CurrencyUtils.formatAsUSD(expense.amount),
                expense.payerId,
                expense.expenseId
        );

        EmbedBuilder embed = paginator.getEmbed(page, itemsPerPage, "Expense List", Color.CYAN, formatter);
        int totalPages = paginator.getTotalPages(itemsPerPage);

        Button prevButton = Button.secondary("expense-list-prev:" + authorId + ":" + targetId + ":" + page, "◀️").withDisabled(page == 0);
        Button nextButton = Button.secondary("expense-list-next:" + authorId + ":" + targetId + ":" + page, "▶️").withDisabled(page >= totalPages - 1);

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .addComponents(ActionRow.of(prevButton, nextButton))
                .build();
    }

    public static MessageCreateData buildSingleExpenseView(int index, String authorId) {
        List<ExpenseData> sortedExpenses = getExpensesSorted();

        if (sortedExpenses.isEmpty())
            return new MessageCreateBuilder().setContent("There are no expenses to view.").build();
        if (index < 0 || index >= sortedExpenses.size())
            return new MessageCreateBuilder().setContent("I think I made a mistake somewhere! :anxious_silvie:").build();

        ExpenseData expense = sortedExpenses.get(index);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.ORANGE);
        embed.setTitle("Expense Details");
        embed.setTimestamp(Instant.ofEpochMilli(expense.getTimestamp()));

        embed.setDescription("### " + expense.name);

        double share = expense.beneficiaryIds.isEmpty()
                ? 0.0
                : expense.amount / expense.beneficiaryIds .size();

        embed.addField("Total Amount", CurrencyUtils.formatAsUSD(expense.amount), true);
        embed.addField("Paid By", "<@" + expense.payerId + ">", true);
        embed.addBlankField(true);
        embed.addField("Share per Person", CurrencyUtils.formatAsUSD(share), true);
        embed.addField("Beneficiaries", String.valueOf(expense.beneficiaryIds.size()), true);
        embed.addBlankField(true);
        embed.addField("Expense ID", "`" + expense.expenseId + "`", false);

        embed.setFooter("Displaying expense " + (index + 1) + " of " + sortedExpenses.size());

        Button prevButton = Button.secondary("expense-view-prev:" + authorId + ":" + index, "◀️ Previous Expense")
                .withDisabled(index == 0);

        Button nextButton = Button.secondary("expense-view-next:" + authorId + ":" + index, "Next Expense ▶️")
                .withDisabled(index >= sortedExpenses.size() - 1);

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .addComponents(ActionRow.of(prevButton, nextButton))
                .build();
    }

}
