package dev.supersand24.expenses;

import dev.supersand24.*;
import dev.supersand24.events.Event;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExpenseManager {

    private static final Logger log = LoggerFactory.getLogger(ExpenseManager.class);

    private static final String EXPENSES_DATA_STORE_NAME = "expenses";
    private static final String DEBTS_DATA_STORE_NAME = "debts";

    private static final int ITEMS_PER_PAGE = 5;

    private static Map<Long, ExpenseData> getExpensesMap() {
        DataPartition<ExpenseData> expenses = DataStore.get(EXPENSES_DATA_STORE_NAME);
        return expenses.getData();
    }

    /**
     * Retrieves a specific event by its ID.
     * @param expenseId The ID of the event to find.
     * @return The Event object, or null if not found.
     */
    private static ExpenseData getExpenseById(long expenseId) {
        DataPartition<ExpenseData> eventPartition = DataStore.get(EXPENSES_DATA_STORE_NAME);
        return eventPartition.getData().get(expenseId);
    }

    private static Map<Long, Debt> getDebtMap() {
        DataPartition<Debt> debts = DataStore.get(DEBTS_DATA_STORE_NAME);
        return debts.getData();
    }

    /**
     * Retrieves a specific event by its ID.
     * @param debtId The ID of the event to find.
     * @return The Debt object, or null if not found.
     */
    private static Debt getDebtById(long debtId) {
        DataPartition<Debt> debtPartition = DataStore.get(DEBTS_DATA_STORE_NAME);
        return debtPartition.getData().get(debtId);
    }

    public static long createExpense(String name, double amount, String payerId, Event event) {
        DataPartition<ExpenseData> expensesHashMap = DataStore.get(EXPENSES_DATA_STORE_NAME);
        long newId = expensesHashMap.getAndIncrementId();
        Map<Long, ExpenseData> expenses = expensesHashMap.getData();
        ExpenseData expense = new ExpenseData(newId, event.getId(), name, amount, payerId);
        expenses.put(newId, expense);
        DataStore.markDirty(EXPENSES_DATA_STORE_NAME);
        return expense.getId();
    }

    public static void deleteExpense(long key) {
        getExpensesMap().remove(key);
        DataStore.markDirty(EXPENSES_DATA_STORE_NAME);
    }

    public static boolean exists(long key) {
        return getExpensesMap().containsKey(key);
    }

    public static String getName(long key) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            return expense.getName();
        } else {
            log.error("Expense " + key + " does not exist.");
            return "???";
        }
    }

    public static void addBenefactors(long key, List<String> benefactorIds) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            for (String id : benefactorIds) {
                if (!expense.getBeneficiaryIds().contains(id)) {
                    expense.getBeneficiaryIds().add(id);
                    DataStore.markDirty(EXPENSES_DATA_STORE_NAME);
                }
            }
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static List<ExpenseData> getExpensesSorted() {
        return getExpensesMap().values().stream()
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static List<ExpenseData> getExpensesForUserSorted(String userId) {
        return getExpensesMap().values().stream()
                .filter(exp -> exp.getPayerId().equals(userId) || exp.getBeneficiaryIds().contains(userId))
                .sorted(Comparator.comparing(ExpenseData::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public static void addPaymentInfo(String userId, String appName, String details) {
        Map<String, List<PaymentInfo>> paymentMethods = DataStore.get("paymentMethods");
        paymentMethods.computeIfAbsent(userId, k -> new ArrayList<>())
                .add(new PaymentInfo(appName, details));
        DataStore.markDirty("paymentMethods");
    }

    public static boolean removePaymentInfo(String userId, String appName) {
        List<PaymentInfo> infos = getPaymentInfoForUser(userId);
        if (infos == null) return false;
        boolean removed = infos.removeIf(info -> info.getAppName().equalsIgnoreCase(appName));
        if (removed) DataStore.markDirty("paymentMethods");
        return removed;
    }

    public static List<PaymentInfo> getPaymentInfoForUser(String userId) {
        Map<String, List<PaymentInfo>> paymentMethods = DataStore.get("paymentMethods");
        return paymentMethods.getOrDefault(userId, Collections.emptyList());
    }

    public static SettlementResult calculateSettlement() {
        List<ExpenseData> unsettledExpenses = getExpensesMap().values().stream()
                .filter(expense -> !expense.isSettled())
                .toList();

        // Get the count of expenses we're about to process
        int processedCount = unsettledExpenses.size();

        if (unsettledExpenses.isEmpty())
            return new SettlementResult(Collections.emptyList(), 0);

        // Step 2: Calculate the net balance for every user.
        Map<String, Double> balances = new HashMap<>();
        for (ExpenseData expense : unsettledExpenses) {
            String payerId = expense.getPayerId();
            double totalAmount = expense.getAmount();
            List<String> beneficiaries = expense.getBeneficiaryIds();

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
            DataStore.markDirty("expenses");
            return new SettlementResult(Collections.emptyList(), 0);
        }

        // Step 4: Generate and store new Debt objects via the simplification algorithm.
        List<Debt> newDebts = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Map.Entry<String, Double> debtorEntry = debtors.getFirst();
            Map.Entry<String, Double> creditorEntry = creditors.getFirst();
            double transferAmount = Math.min(Math.abs(debtorEntry.getValue()), creditorEntry.getValue());

            // Create a new persistent Debt object

            DataPartition<Debt> debtsHashMap = DataStore.get("debts");
            long newDebtId = debtsHashMap.getAndIncrementId();
            Debt newDebt = new Debt(newDebtId, 0, debtorEntry.getKey(), creditorEntry.getKey(), transferAmount);

            // Store it in our main data store and add to a temporary list to return
            debtsHashMap.getData().put(newDebtId, newDebt);
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
        DataStore.markDirty("debts");
        return new SettlementResult(newDebts, processedCount);
    }

    public static List<Debt> getOutstandingDebts() {
        return getDebtMap().values().stream()
                .filter(debt -> !debt.isPaid())
                .sorted(Comparator.comparing(Debt::getDebtId))
                .collect(Collectors.toList());
    }

    public static String markDebtAsPaid(long debtId, String actioningUserId) {
        Debt debt = getDebtMap().get(debtId);
        if (debt == null) return "This debt doesn't exist in my library!";
        if (debt.isPaid()) return "You already settled this debt.";

        if (!debt.getCreditorId().equals(actioningUserId)) {
            return "You aren't (<@" + debt.getCreditorId() + ">)." + ArisannaBot.emojiBonkArisanna;
        }

        debt.markAsPaid();
        DataStore.markDirty("debts");

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

        return new MessageCreateBuilder()
                .addComponents(buildExpenseListContainer(expenses, page, authorId))
                .useComponentsV2()
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

    public static MessageCreateData generateExpenseListMessage(String authorId, int page) {
        List<ExpenseData> expenses = ExpenseManager.getExpensesSorted();
        if (expenses.isEmpty()) {
            return new MessageCreateBuilder().setContent("No expenses found matching criteria.").build();
        }
        return new MessageCreateBuilder()
                .addComponents(buildExpenseListContainer(expenses, page, authorId))
                .useComponentsV2()
                .build();
    }

    public static MessageCreateData generateExpenseDetailMessage(String authorId, int index) {
        return new MessageCreateBuilder()
                .addComponents(buildExpenseDetailContainer(index, authorId))
                .useComponentsV2()
                .build();
    }

    public static MessageCreateData generateDebtListMessage(String authorId, int page) {
        return new MessageCreateBuilder()
                .addComponents(buildDebtListContainer(page, authorId))
                .useComponentsV2()
                .build();
    }

    public static MessageCreateData generateDebtDetailMessage(String authorId, int index, JDA jda) {
        return new MessageCreateBuilder()
                .addComponents(buildDebtDetailContainer(index, authorId, jda))
                .useComponentsV2()
                .build();
    }

    public static Container buildExpenseListContainer(List<ExpenseData> expenses, int page, String authorId) {
        int totalPages = (int) Math.ceil((double) expenses.size() / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of All Expenses"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        //Add Text Display for current filter here

        Function<ExpenseData, String> formatter = (expense) -> String.format(
                "**%s** - %s\nPaid by <@%s> | ID: `%s`\n\n",
                expense.getName(),
                CurrencyUtils.formatAsUSD(expense.getAmount()),
                expense.getPayerId(),
                expense.getId()
        );

        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < expenses.size(); i++) {
            ExpenseData expense = expenses.get(startIndex + i);
            components.add(TextDisplay.of(formatter.apply(expense)));
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, "expense-list-zoom:" + authorId + ":" + expense.getId(), "Details")));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(buildExpenseListActionRow(expenses, authorId, page));

        return Container.of(components);
    }

    private static ActionRow buildExpenseListActionRow(List<ExpenseData> expenses, String authorId, int page) {
        int totalPages = (int) Math.ceil((double) expenses.size() / ITEMS_PER_PAGE);

        Button prev = Button.secondary("expense-list-prev:" + authorId + ":" + page, "◀️ Previous").withDisabled(page == 0);
        Button next = Button.secondary("expense-list-next:" + authorId + ":" + page, "Next ▶️").withDisabled(page >= totalPages - 1);

        return ActionRow.of(prev, next);
    }

    public static Container buildExpenseDetailContainer(int index, String authorId) {
        ExpenseData expense = getExpenseById(index);

        if (expense == null) {
            log.error("Could not find Expense # {} to show Details.", index);
            return buildExpenseListContainer(ExpenseManager.getExpensesSorted(), 0, authorId);
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Expense Details: " + expense.getName()));

        double share = expense.getBeneficiaryIds().isEmpty()
                ? 0.0
                : expense.getAmount() / expense.getBeneficiaryIds().size();

        components.add(TextDisplay.of("### Total Amount: " + CurrencyUtils.formatAsUSD(expense.getAmount())));
        components.add(TextDisplay.of("### Paid By: <@" + expense.getPayerId() + ">"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("### Share per Person: " + CurrencyUtils.formatAsUSD(share)));
        components.add(TextDisplay.of("### Beneficiaries: " + expense.getBeneficiaryIds().size()));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("-# Expense ID: " + expense.getId()));
        components.add(ActionRow.of(
                Button.primary("expense-edit:" + authorId + ":" + index, "Edit"),
                Button.danger("expense-detail-back:" + authorId, "List")
        ));

        return Container.of(components);
    }

    private static Container buildDebtListContainer(int page, String authorId) {
        List<Debt> outstandingDebts = getOutstandingDebts();
        int totalPages = (int) Math.ceil((double) outstandingDebts.size() / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of All Outstanding Debts"));

        if (outstandingDebts.isEmpty())
            components.add(TextDisplay.of("All debts are settled! There are no outstanding payments."));
        else {
            components.add(TextDisplay.of("Here is the current list of unpaid debts. The creditor should run `/debt markpaid` once they receive payment."));
            for (Debt debt : outstandingDebts) {
                components.add(TextDisplay.of(String.format(
                        "• <@%s> owes <@%s> **%s**\n  (ID: `%d`)\n",
                        debt.getDebtorId(),
                        debt.getCreditorId(),
                        CurrencyUtils.formatAsUSD(debt.getAmount()),
                        debt.getDebtId()
                )));
            }
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));

        return Container.of(components);
    }

    private static Container buildDebtDetailContainer(int index, String authorId, JDA jda) {
        Debt debt = getDebtById(index);

        User debtor = jda.retrieveUserById(debt.getDebtorId()).complete();
        User creditor = jda.retrieveUserById(debt.getCreditorId()).complete();
        //Event event = EventManager.getEventById(debt.getEventId());

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Debt Details: " + debtor.getName() + " → " + creditor.getName()));
        components.add(TextDisplay.of("Amount Owed\n" + CurrencyUtils.formatAsUSD(debt.getAmount())));

        List<PaymentInfo> paymentInfos = ExpenseManager.getPaymentInfoForUser(debt.getCreditorId());
        if (paymentInfos.isEmpty())
            components.add(TextDisplay.of("This user has not added any payment information."));
        else {
            components.add(TextDisplay.of("How to Pay " + creditor.getName()));
            for (PaymentInfo info : paymentInfos) {
                components.add(TextDisplay.of(String.format("**%s:** `%s`\n", info.getAppName(), info.getDetail())));
            }
        }

        return Container.of(components);
    }

    public static Container buildPaymentInfoDetailContainer() {
        SettlementResult result = calculateSettlement();

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Settlement Plan"));

        if (result.newDebts().isEmpty()) {
            if (result.expensesProcessedCount() > 0) {
                components.add(TextDisplay.of("All " + result.expensesProcessedCount() + " unsettled expenses for this event have been calculated and balanced out.\nNo new payments are needed!"));
            } else {
                components.add(TextDisplay.of("There were no new expenses to settle for this event."));
            }
        } else {
            if (result.expensesProcessedCount() == 1) {
                components.add(TextDisplay.of("A new payment plan has been generated. **The 1 expense included in this calculation is now considered settled** and will not be part of future settlements."));
            } else {
                components.add(TextDisplay.of("A new payment plan has been generated. **The " + result.expensesProcessedCount() + " expenses included in this calculation are now considered settled** and will not be part of future settlements."));
            }

            StringBuilder paymentPlan = new StringBuilder();
            for(Debt debt : result.newDebts()) {
                components.add(TextDisplay.of(String.format(
                        "• <@%s> owes <@%s> **%s**\n",
                        debt.getDebtorId(),
                        debt.getCreditorId(),
                        CurrencyUtils.formatAsUSD(debt.getAmount())
                )));
            }

            components.add(TextDisplay.of("-# What's next? Use /debt list and /debt markpaid to complete payments."));
        }

        Button explanationButton = Button.secondary("settleup-explain", "How is this calculated?")
                .withEmoji(ArisannaBot.emojiLoadingArisanna);

        components.add(ActionRow.of(explanationButton));

        return Container.of(components);
    }

}
