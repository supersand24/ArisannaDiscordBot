package dev.supersand24.expenses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.supersand24.ArisannaBot;
import dev.supersand24.CurrencyUtils;
import dev.supersand24.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExpenseManager {

    private static ExpenseDataStore dataStore;
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
                dataStore = gson.fromJson(json, ExpenseDataStore.class);

                if (dataStore == null) dataStore = new ExpenseDataStore();

                for (Map.Entry<Long, ExpenseData> entry : dataStore.getExpenses().entrySet()) {
                    ExpenseData expenseData = entry.getValue();
                    expenseData.expenseId = entry.getKey();
                }
            }
        } catch (IOException e) {
            log.error("Failed to load counters: " + e.getMessage());
            dataStore = new ExpenseDataStore();
        }
    }

    public static void saveExpenses() {
        try {
            String json = gson.toJson(dataStore);
            Files.writeString(filePath, json);
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

    public static long createExpense(String name, double amount, String payerId) {
        long newId = dataStore.getAndIncrementNextExpenseId();
        ExpenseData expense = new ExpenseData(newId, name, amount, payerId);
        dataStore.getExpenses().put(newId, expense);

        saveExpenses();
        return expense.expenseId;
    }

    public static void deleteExpense(long key) {
        dataStore.getExpenses().remove(key);
        dirty = true;
    }

    public static boolean exists(long key) {
        return dataStore.getExpenses().containsKey(key);
    }

    public static String getName(long key) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            return expense.name;
        } else {
            log.error("Expense " + key + " does not exist.");
            return "???";
        }
    }

    public static void setAmount(long key, double amount) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            expense.amount = amount;
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static double getAmount(long key) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            return expense.amount;
        } else {
            log.error("Expense " + key + " does not exist.");
            return 0;
        }
    }

    public static boolean isBenefitingFromExpense(long key, String userId) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            return expense.beneficiaryIds.contains(userId);
        } else {
            log.error("Expense " + key + " does not exist.");
            return false;
        }
    }

    public static void addBenefactor(long key, String benefactorId) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            expense.beneficiaryIds.add(benefactorId);
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void addBenefactors(long key, List<String> benefactorIds) {
        ExpenseData expense = dataStore.getExpenses().get(key);
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

    public static void removeBenefactor(long key, String benefactorId) {
        ExpenseData expense = dataStore.getExpenses().get(key);
        if (expense != null) {
            expense.beneficiaryIds.remove(benefactorId);
            dirty = true;
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void removeBenefactors(long key, List<String> benefactorIds) {
        ExpenseData expense = dataStore.getExpenses().get(key);
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

    public static List<ExpenseData> getExpensesSorted() {
        return dataStore.getExpenses().values().stream()
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static List<ExpenseData> getExpensesForUserSorted(String userId) {
        return dataStore.getExpenses().values().stream()
                .filter(exp -> exp.payerId.equals(userId) || exp.beneficiaryIds.contains(userId))
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static void addPaymentInfo(String userId, String appName, String details) {
        dataStore.getPaymentDetails().computeIfAbsent(userId, k -> new ArrayList<>())
                .add(new PaymentInfo(appName, details));
        dirty = true;
    }

    public static boolean removePaymentInfo(String userUd, String appName) {
        List<PaymentInfo> infos = dataStore.getPaymentDetails().get(userUd);
        if (infos == null) return false;
        boolean removed = infos.removeIf(info -> info.getAppName().equalsIgnoreCase(appName));
        if (removed) dirty = true;
        return removed;
    }

    public static List<PaymentInfo> getPaymentInfoForUser(String userId) {
        return dataStore.getPaymentDetails().getOrDefault(userId, Collections.emptyList());
    }

    public static SettlementResult calculateSettlement() {
        List<ExpenseData> unsettledExpenses = dataStore.getExpenses().values().stream()
                .filter(expense -> !expense.isSettled())
                .toList();

        // Get the count of expenses we're about to process
        int processedCount = unsettledExpenses.size();

        if (unsettledExpenses.isEmpty())
            return new SettlementResult(Collections.emptyList(), 0);

        // Step 2: Calculate the net balance for every user.
        Map<String, Double> balances = new HashMap<>();
        for (ExpenseData expense : unsettledExpenses) {
            String payerId = expense.payerId;
            double totalAmount = expense.amount;
            List<String> beneficiaries = expense.beneficiaryIds;

            if (beneficiaries == null || beneficiaries.isEmpty()) continue;

            balances.put(payerId, balances.getOrDefault(payerId, 0.0) + totalAmount);
            double share = totalAmount / beneficiaries.size();
            for (String beneficiaryId : beneficiaries)
                balances.put(beneficiaryId, balances.getOrDefault(beneficiaryId, 0.0) - share);
        }

        // Step 3: Separate users into Debtors (owe money) and Creditors (are owed money).
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            if (entry.getValue() > 0.01) creditors.add(entry);
            else if (entry.getValue() < -0.01) debtors.add(entry);
        }

        if (debtors.isEmpty() || creditors.isEmpty()) {
            unsettledExpenses.forEach(ExpenseData::setSettled);
            dirty = true;
            return new SettlementResult(Collections.emptyList(), 0);
        }

        // Step 4: Generate and store new Debt objects via the simplification algorithm.
        List<Debt> newDebts = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Map.Entry<String, Double> debtorEntry = debtors.getFirst();
            Map.Entry<String, Double> creditorEntry = creditors.getFirst();
            double transferAmount = Math.min(Math.abs(debtorEntry.getValue()), creditorEntry.getValue());

            // Create a new persistent Debt object
            long newDebtId = dataStore.getAndIncrementNextDebtId();
            Debt newDebt = new Debt(newDebtId, debtorEntry.getKey(), creditorEntry.getKey(), transferAmount);

            // Store it in our main data store and add to a temporary list to return
            dataStore.getDebts().put(newDebtId, newDebt);
            newDebts.add(newDebt);

            // Update balances for the next loop iteration
            debtorEntry.setValue(debtorEntry.getValue() + transferAmount);
            creditorEntry.setValue(creditorEntry.getValue() - transferAmount);

            // If a debtor's balance is now effectively zero, remove them from the list.
            if (Math.abs(debtorEntry.getValue()) < 0.01)
                debtors.removeFirst();

            // If a creditor's balance is now effectively zero, remove them from the list.
            if (creditorEntry.getValue() < 0.01)
                creditors.removeFirst();
        }

        // Step 5: Mark the processed expenses as settled.
        for (ExpenseData expense : unsettledExpenses)
            expense.setSettled();

        // Step 6: Persist all changes and return.
        dirty = true;
        return new SettlementResult(newDebts, processedCount);
    }

    public static List<Debt> getOutstandingDebts() {
        return dataStore.getDebts().values().stream()
                .filter(debt -> !debt.isPaid())
                .sorted(Comparator.comparing(Debt::getDebtId))
                .collect(Collectors.toList());
    }

    public static String markDebtAsPaid(long debtId, String actioningUserId) {
        Debt debt = dataStore.getDebts().get(debtId);
        if (debt == null) return "This debt doesn't exist in my library!";
        if (debt.isPaid()) return "You already settled this debt.";

        if (!debt.getCreditorId().equals(actioningUserId)) {
            return "You aren't (<@" + debt.getCreditorId() + ">)." + ArisannaBot.emojiBonkArisanna;
        }

        debt.markAsPaid();
        dirty = true;

        return String.format("Success! Debt #%d (%s owed by <@%s>) has been marked as paid.",
                debtId, CurrencyUtils.formatAsUSD(debt.getAmount()), debt.getDebtorId());
    }

    public static MessageCreateData buildSettlementView() {
        SettlementResult result = calculateSettlement();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.GREEN);
        embed.setTitle("Settlement Plan");
        embed.setTimestamp(Instant.now());

        if (result.newDebts().isEmpty()) {
            if (result.expensesProcessedCount() > 0) {
                embed.setDescription("All " + result.expensesProcessedCount() + " unsettled expenses for this event have been calculated and balanced out. No new payments are needed!");
            } else {
                embed.setDescription("There were no new expenses to settle for this event.");
            }
        } else {
            if (result.expensesProcessedCount() == 1)
                embed.setDescription("A new payment plan has been generated. **The 1 expense included in this calculation is now considered settled** and will not be part of future settlements.");
            else
                embed.setDescription("A new payment plan has been generated. **The " + result.expensesProcessedCount() + " expenses included in this calculation are now considered settled** and will not be part of future settlements.");

            StringBuilder paymentPlan = new StringBuilder();
            for(Debt debt : result.newDebts()) {
                paymentPlan.append(String.format(
                        "• <@%s> owes <@%s> **%s**\n",
                        debt.getDebtorId(),
                        debt.getCreditorId(),
                        CurrencyUtils.formatAsUSD(debt.getAmount())
                ));
            }
            embed.addField("New Payment Plan", paymentPlan.toString(), false);
            embed.setFooter("What's next? Use /debt list and /debt markpaid to complete payments.");
        }

        Button explanationButton = Button.secondary("settleup-explain", "How is this calculated?")
                .withEmoji(ArisannaBot.emojiLoadingArisanna);

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .addComponents(ActionRow.of(explanationButton))
                .build();
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

    public static MessageCreateData buildPaymentMethodView(User targetUser) {
        List<PaymentInfo> paymentInfos = getPaymentInfoForUser(targetUser.getId());
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(targetUser.getName() + " Payment Methods", null, targetUser.getEffectiveAvatarUrl());
        embed.setColor(Color.MAGENTA);

        if (paymentInfos.isEmpty())
            embed.setDescription(targetUser.getAsMention() + " has not added any payment methods yet.");
        else {
            embed.setDescription("Here are their saved payment methods. Use this info to settle up any debts.");
            for (PaymentInfo info : paymentInfos)
                embed.addField(info.getAppName(), "`" + info.getDetail() + "`", false);
        }

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .build();
    }

    public static MessageCreateData buildDebtList() {
        List<Debt> outstandingDebts = getOutstandingDebts();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Outstanding Debts");
        embed.setColor(Color.RED);
        embed.setTimestamp(Instant.now());

        if (outstandingDebts.isEmpty())
            embed.setDescription("All debts are settled! There are no outstanding payments.");
        else {
            StringBuilder description = new StringBuilder("Here is the current list of unpaid debts. The creditor should run `/debt markpaid` once they receive payment.\n\n");
            for (Debt debt : outstandingDebts) {
                description.append(String.format(
                        "• <@%s> owes <@%s> **%s**\n  (ID: `%d`)\n",
                        debt.getDebtorId(),
                        debt.getCreditorId(),
                        CurrencyUtils.formatAsUSD(debt.getAmount()),
                        debt.getDebtId()
                ));
            }
            embed.setDescription(description.toString());
        }

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .build();
    }

}
