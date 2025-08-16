package dev.supersand24.expenses;

import dev.supersand24.*;
import dev.supersand24.events.Event;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder;
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

    private static Map<Long, ExpenseData> getExpensesMap() {
        DataPartition<ExpenseData> expenses = DataStore.get("expenses");
        return expenses.getData();
    }

    private static Map<Long, Debt> getDebtMap() {
        DataPartition<Debt> debts = DataStore.get("debts");
        return debts.getData();
    }

    public static long createExpense(String name, double amount, String payerId, Event event) {
        DataPartition<ExpenseData> expensesHashMap = DataStore.get("expenses");
        long newId = expensesHashMap.getAndIncrementId();
        Map<Long, ExpenseData> expenses = expensesHashMap.getData();
        ExpenseData expense = new ExpenseData(newId, event.getId(), name, amount, payerId);
        expenses.put(newId, expense);
        DataStore.markDirty("expenses");
        return expense.expenseId;
    }

    public static void deleteExpense(long key) {
        getExpensesMap().remove(key);
        DataStore.markDirty("expenses");
    }

    public static boolean exists(long key) {
        return getExpensesMap().containsKey(key);
    }

    public static String getName(long key) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            return expense.name;
        } else {
            log.error("Expense " + key + " does not exist.");
            return "???";
        }
    }

    public static void setAmount(long key, double amount) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            expense.amount = amount;
            DataStore.markDirty("expenses");
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static double getAmount(long key) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            return expense.amount;
        } else {
            log.error("Expense " + key + " does not exist.");
            return 0;
        }
    }

    public static boolean isBenefitingFromExpense(long key, String userId) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            return expense.beneficiaryIds.contains(userId);
        } else {
            log.error("Expense " + key + " does not exist.");
            return false;
        }
    }

    public static void addBenefactor(long key, String benefactorId) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            expense.beneficiaryIds.add(benefactorId);
            DataStore.markDirty("expenses");
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void addBenefactors(long key, List<String> benefactorIds) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            for (String id : benefactorIds) {
                if (!expense.beneficiaryIds.contains(id)) {
                    expense.beneficiaryIds.add(id);
                    DataStore.markDirty("expenses");
                }
            }
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void removeBenefactor(long key, String benefactorId) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            expense.beneficiaryIds.remove(benefactorId);
            DataStore.markDirty("expenses");
        } else {
            log.error("Expense " + key + " does not exist.");
        }
    }

    public static void removeBenefactors(long key, List<String> benefactorIds) {
        ExpenseData expense = getExpensesMap().get(key);
        if (expense != null) {
            for (String id : benefactorIds) {
                if (expense.beneficiaryIds.contains(id)) {
                    expense.beneficiaryIds.remove(id);
                    DataStore.markDirty("expenses");
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
                .filter(exp -> exp.payerId.equals(userId) || exp.beneficiaryIds.contains(userId))
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

        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .addComponents(buildExpenseListActionRow(expenses, authorId, page))
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

    //New Methods

    public static MessageCreateData editExpenseListView(String authorId, int page) {
        List<ExpenseData> expenses = ExpenseManager.getExpensesSorted();
        return new MessageCreateBuilder()
                .addEmbeds(createExpenseListEmbed(expenses, page).build())
                .setComponents(buildExpenseListActionRow(expenses, authorId, page))
                .build();
    }

    public static MessageCreateData editDebtListView(String authorId, int page) {
        List<Debt> debts = ExpenseManager.getOutstandingDebts();
        return new MessageCreateBuilder()
                .addEmbeds(createDebtListEmbed(debts, page).build())
                .setComponents(buildDebtListActionRow(debts, authorId, page))
                .build();
    }

    public static MessageCreateData editExpenseDetailView(String authorId, int index) {
        List<ExpenseData> expenses = ExpenseManager.getExpensesSorted();
        ExpenseData expense = expenses.get(index);
        EmbedBuilder embed = expense.createEmbed()
                .setFooter("Displaying expense " + (index + 1) + " of " + expenses.size());
        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .setComponents(buildExpenseDetailActionRow(expenses, authorId, index))
                .build();
    }

    public static MessageCreateData editDebtDetailView(String authorId, int index, JDA jda) {
        List<Debt> debts = ExpenseManager.getOutstandingDebts();
        Debt debt = debts.get(index);
        EmbedBuilder embed = debt.createEmbed(jda)
                .setFooter("Displaying debt " + (index + 1) + " of " + debts.size());
        return new MessageCreateBuilder()
                .addEmbeds(embed.build())
                .setComponents(buildDebtDetailActionRow(debts, authorId, index))
                .build();
    }

    private static EmbedBuilder createExpenseListEmbed(List<ExpenseData> expenses, int page) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) expenses.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("List of All Expenses");
        embed.setColor(Color.MAGENTA);
        embed.setFooter("Page " + (page + 1) + " of " + totalPages);

        Function<ExpenseData, String> formatter = (expense) -> String.format(
                "**%s** - %s\nPaid by <@%s> | ID: `%s`\n\n",
                expense.name,
                CurrencyUtils.formatAsUSD(expense.amount),
                expense.payerId,
                expense.expenseId
        );

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < itemsPerPage && (startIndex + i) < expenses.size(); i++) {
            ExpenseData expense = expenses.get(startIndex + i);
            description.append(formatter.apply(expense)).append("\n");
        }
        embed.setDescription(description.toString());
        return embed;
    }

    private static EmbedBuilder createDebtListEmbed(List<Debt> debts, int page) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) debts.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Outstanding Debts");
        embed.setColor(Color.MAGENTA);
        embed.setFooter("Page " + (page + 1) + " of " + totalPages);

        Function<ExpenseData, String> formatter = (expense) -> String.format(
                "**%s** - %s\nPaid by <@%s> | ID: `%s`\n\n",
                expense.name,
                CurrencyUtils.formatAsUSD(expense.amount),
                expense.payerId,
                expense.expenseId
        );

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < itemsPerPage && (startIndex + i) < debts.size(); i++) {
            Debt debt = debts.get(startIndex + i);
            description.append(String.format("`%d`: <@%s> owes <@%s> **%s**\n",
                    debt.getDebtId(), debt.getDebtorId(), debt.getCreditorId(), CurrencyUtils.formatAsUSD(debt.getAmount())));
        }
        embed.setDescription(description.toString());
        return embed;
    }

    private static List<ActionRow> buildExpenseListActionRow(List<ExpenseData> expenses, String authorId, int page) {
        int totalPages = (int) Math.ceil((double) expenses.size() / 5.0);
        int expenseIndex = page * 5;

        Button prev = Button.secondary("expense-list-prev:" + authorId + ":" + page, "◀️ Previous Page").withDisabled(page == 0);
        Button next = Button.secondary("expense-list-next:" + authorId + ":" + page, "Next Page ▶️").withDisabled(page >= totalPages - 1);

        Builder menu = StringSelectMenu.create("expense-list-zoom:" + authorId)
                .setPlaceholder("View details for a specific expense...");

        int startIndex = page * 5;
        for (int i = 0; i < 5 && (startIndex + i) < expenses.size(); i++) {
            ExpenseData expense = expenses.get(startIndex + i);
            menu.addOption(expense.getName(), String.valueOf(startIndex + i));
        }

        return List.of(ActionRow.of(prev, next), ActionRow.of(menu.build()));
    }

    private static List<ActionRow> buildExpenseDetailActionRow(List<ExpenseData> expenses, String authorId, int index) {
        int page = index / 5;

        Button prev = Button.secondary("expense-detail-prev:" + authorId + ":" + index, "◀️ Previous Expense").withDisabled(index == 0);
        Button next = Button.secondary("expense-detail-next:" + authorId + ":" + index, "Next Expense ▶️").withDisabled(index >= expenses.size() - 1);
        Button back = Button.danger("expense-detail-back:" + authorId + ":" + page, "Back to List");

        return List.of(ActionRow.of(prev, next), ActionRow.of(back));
    }

    private static List<ActionRow> buildDebtListActionRow(List<Debt> debts, String authorId, int page) {
        int totalPages = (int) Math.ceil((double) debts.size() / 5.0);

        Button prev = Button.secondary("debt-list-prev:" + authorId + ":" + page, "◀️ Previous Page").withDisabled(page == 0);
        Button next = Button.secondary("debt-list-next:" + authorId + ":" + page, "Next Page ▶️").withDisabled(page >= totalPages - 1);

        Builder menu = StringSelectMenu.create("debt-list-zoom:" + authorId)
                .setPlaceholder("View details for a specific debt...");

        int startIndex = page * 5;
        for (int i = 0; i < 5 && (startIndex + i) < debts.size(); i++) {
            Debt debt = debts.get(startIndex + i);
            menu.addOption(String.format("ID %d: ...owes... %s", debt.getDebtId(), CurrencyUtils.formatAsUSD(debt.getAmount())), String.valueOf(startIndex + i));
        }

        return List.of(ActionRow.of(prev, next), ActionRow.of(menu.build()));
    }

    private static List<ActionRow> buildDebtDetailActionRow(List<Debt> debts, String authorId, int index) {
        int page = index / 5;

        Button prev = Button.secondary("debt-detail-prev:" + authorId + ":" + index, "◀️ Previous Debt").withDisabled(index == 0);
        Button next = Button.secondary("debt-detail-next:" + authorId + ":" + index, "Next Debt ▶️").withDisabled(index >= debts.size() - 1);
        Button back = Button.danger("debt-detail-back:" + authorId + ":" + page, "Back to List");

        return List.of(ActionRow.of(prev, next), ActionRow.of(back));
    }

}
