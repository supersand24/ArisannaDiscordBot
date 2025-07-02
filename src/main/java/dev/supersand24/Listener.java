package dev.supersand24;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.supersand24.counters.CounterManager;
import dev.supersand24.expenses.Debt;
import dev.supersand24.expenses.ExpenseData;
import dev.supersand24.expenses.ExpenseManager;
import dev.supersand24.expenses.PaymentInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

public class Listener extends ListenerAdapter {

    private final Logger log = LoggerFactory.getLogger(Listener.class);

    @Override
    public void onReady(ReadyEvent ev) {
        log.info("Listener is Ready.");
    }

    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("counter")) {
            List<Command.Choice> options = CounterManager.getCounterNames().stream()
                    .filter(counterName -> counterName.startsWith(e.getFocusedOption().getValue()))
                    .map(counterName -> new Command.Choice(counterName, counterName))
                    .collect(Collectors.toList());
            e.replyChoices(options).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {

        User commandUser = e.getUser();

        switch (e.getName()) {
            case "counter" -> {

                //If trying to create a Counter, handle first
                if (e.getFullCommandName().equals("counter create")) {
                    String optionName = e.getOption("name").getAsString();
                    if (CounterManager.getCounterNames().contains(optionName)) {
                        e.reply(optionName + " counter already exists!").setEphemeral(true).queue();
                    } else {
                        String optionDescription = e.getOption("description").getAsString();
                        int initialValue = e.getOption("initial-value") != null ? e.getOption("initial-value").getAsInt() : 0;
                        int minValue = e.getOption("min-value") != null ? e.getOption("min-value").getAsInt() : 0;
                        int maxValue = e.getOption("max-value") != null ? e.getOption("max-value").getAsInt() : Integer.MAX_VALUE;
                        CounterManager.createCounter(optionName, optionDescription, initialValue, minValue, maxValue, commandUser.getId());
                        e.reply(optionName + " counter was created!").queue();
                    }
                    return;
                }

                String counterName = e.getOption("counter").getAsString();

                //Check to see if Counter exists
                if (!CounterManager.getCounterNames().contains(counterName)) {
                    e.reply("I can't find a counter named " + counterName + "!").setEphemeral(true).queue();
                    return;
                }

                //Check to see if user has editing access
                if (!CounterManager.canEdit(counterName, commandUser.getId())) {
                    e.reply("You don't have editing access on " + counterName + " counter.").setEphemeral(true).queue();
                    return;
                }

                //Different Command Groups
                switch (e.getSubcommandGroup()) {
                    case null -> {
                        switch (e.getSubcommandName()) {
                            case "increment" -> {
                                CounterManager.increment(counterName);
                                e.reply(counterName + " counter incremented to " + CounterManager.getValue(counterName) + ".").queue();
                            }
                            case "decrement" -> {
                                CounterManager.decrement(counterName);
                                e.reply(counterName + " counter decremented to " + CounterManager.getValue(counterName) + ".").queue();
                            }
                            case "set" -> {
                                int value = e.getOption("value").getAsInt();
                                CounterManager.setValue(counterName, value);
                                e.reply(counterName + " counter set to " + value + ".").queue();
                            }
                            case "display" -> e.replyEmbeds(CounterManager.getCounterEmbed(counterName)).queue();
                            case "delete" -> {
                                CounterManager.deleteCounter(counterName);
                                e.reply(counterName + " counter was deleted!").queue();
                            }
                        }
                    }
                    case "editor" -> {
                        User editor = e.getOption("editor").getAsUser();

                        switch (e.getSubcommandName()) {
                            case "add" -> {
                                if (CounterManager.canEdit(counterName, editor.getId())) {
                                    e.reply(editor.getName() + " is already authorized on " + counterName + " counter.").setEphemeral(true).queue();
                                } else {
                                    CounterManager.addEditor(counterName, editor.getId());
                                    e.reply(editor.getName() + " is now an editor of " + counterName + " counter.").setEphemeral(true).queue();
                                }
                            }
                            case "remove" -> {
                                if (CounterManager.canEdit(counterName, editor.getId())) {
                                    CounterManager.removeEditor(counterName, editor.getId());
                                    e.reply(editor.getName() + " is no longer an editor of " + counterName + " counter.").setEphemeral(true).queue();
                                } else {
                                    e.reply(editor.getName() + " is not currently authorized on " + counterName + " counter.").setEphemeral(true).queue();
                                }
                            }
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + e.getSubcommandGroup());
                }
            }
            case "expense" -> {

                switch (e.getSubcommandName()) {
                    case "add" -> {
                        String optionName = e.getOption("name").getAsString();
                        double optionAmount = e.getOption("amount").getAsDouble();

                        long expenseId = ExpenseManager.createExpense(optionName, optionAmount, e.getUser().getId());

                        e.reply("Created " + CurrencyUtils.formatAsUSD(optionAmount) + " expense.\nChoose who benefited from " + optionName + ".")
                                .addActionRow(
                                        EntitySelectMenu.create("expense-beneficiary-select:" + expenseId, EntitySelectMenu.SelectTarget.USER)
                                                .setDefaultValues(EntitySelectMenu.DefaultValue.user(e.getUser().getId()))
                                                .setMaxValues(20)
                                                .build()
                                ).setEphemeral(true).queue();
                    }
                    case "remove" -> {
                        e.reply("coming soon.").setEphemeral(true).queue();
                    }
                    case "view" -> {
                        long expenseId = e.getOption("id") != null ? e.getOption("id").getAsLong() : -1;
                        e.deferReply().queue();

                        List<ExpenseData> sortedExpenses = ExpenseManager.getExpensesSorted();
                        int initialIndex = -1;

                        for (int i = 0; i < sortedExpenses.size(); i++) {
                            if (sortedExpenses.get(i).expenseId == expenseId) {
                                initialIndex = i;
                                break;
                            }
                        }

                        if (initialIndex == -1) {
                            e.getHook().sendMessage("This expense doesn't exist in my library!").setEphemeral(true).queue();
                            return;
                        }

                        MessageCreateData messageData = ExpenseManager.buildSingleExpenseView(initialIndex, e.getUser().getId());
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "list" -> {
                        e.deferReply().queue();
                        User userFilter = e.getOption("user") != null ? e.getOption("user").getAsUser() : null;
                        String targetId = (userFilter == null) ? "all" : userFilter.getId();

                        MessageCreateData messageData = ExpenseManager.buildExpenseListPage(0, e.getUser().getId(), targetId);
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "settleup" -> {
                        e.deferReply().queue();
                        MessageCreateData messageData = ExpenseManager.buildSettlementView();
                        e.getHook().sendMessage(messageData).queue();
                    }
                }
            }
            case "payment" -> {
                switch (e.getSubcommandName()) {
                    case "add" -> {
                        String appName = e.getOption("app").getAsString();
                        String details = e.getOption("details").getAsString();
                        String userId = e.getUser().getId();

                        ExpenseManager.addPaymentInfo(userId, appName, details);

                        e.reply("Successfully added payment method:\n**" + appName + ":** `" + details + "`")
                                .setEphemeral(true)
                                .queue();
                    }
                    case "remove" -> {
                        String appName = e.getOption("app").getAsString();
                        String userId = e.getUser().getId();

                        boolean wasRemoved = ExpenseManager.removePaymentInfo(userId, appName);

                        if (wasRemoved) {
                            e.reply("I removed your **" + appName + "** information from my library.").setEphemeral(true).queue();
                        } else {
                            e.reply("I could not find your **" + appName + "** information to from my library.").setEphemeral(true).queue();
                        }
                    }
                    case "view" -> {
                        e.deferReply().queue();
                        User targetUser = e.getOption("user").getAsUser();
                        MessageCreateData messageData = ExpenseManager.buildPaymentMethodView(targetUser);
                        e.getHook().sendMessage(messageData).queue();
                    }
                }
            }
            case "debt" -> {
                switch (e.getSubcommandName()) {
                    case "list" -> {
                        e.deferReply().queue();
                        MessageCreateData messageData = ExpenseManager.buildDebtList();
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "markpaid" -> {
                        long debtId = e.getOption("id").getAsLong();
                        String actioningUserId = e.getUser().getId();
                        String resultMessage = ExpenseManager.markDebtAsPaid(debtId, actioningUserId);
                        e.reply(resultMessage).setEphemeral(true).queue();
                    }
                }
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent e) {
        log.info(e.getComponentId() + " was interacted with.");

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.equals("expense-beneficiary-select")) {
            long expenseId = Long.parseLong(parts[1]);

            if (!ExpenseManager.exists(expenseId)) {
                log.error("Could not find expense: " + expenseId);
                e.reply("This expense doesn't exist in my library!").setEphemeral(true).queue();
                return;
            }

            List<String> beneficiaryIds = e.getMentions().getUsers().stream()
                    .map(IMentionable::getId)
                    .toList();

            ExpenseManager.addBenefactors(expenseId, beneficiaryIds);

            if (beneficiaryIds.size() == 1) {
                e.reply("Added 1 person to " + ExpenseManager.getName(expenseId) + " expense.").setEphemeral(true).queue();
            } else {
                e.reply("Added " + beneficiaryIds.size() + " people to " + ExpenseManager.getName(expenseId) + " expense.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        log.info(e.getComponentId() + " was pressed.");

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.startsWith("expense-list-")) {
            String authorId = parts[1];
            String expenseId = parts[2];
            int currentPage = Integer.parseInt(parts[3]);

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();
            int newPage = prefix.endsWith("-next") ? currentPage + 1 : currentPage - 1;

            MessageCreateData messageData = ExpenseManager.buildExpenseListPage(newPage, authorId, expenseId);
            e.getHook().editOriginalEmbeds(messageData.getEmbeds())
                    .setComponents(messageData.getComponents())
                    .queue();

        } else if (prefix.startsWith("expense-view-")) {
            String authorId = parts[1];
            int currentIndex = Integer.parseInt(parts[2]);

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();
            int newIndex = prefix.endsWith("-next") ? currentIndex + 1 : currentIndex - 1;

            MessageCreateData messageData = ExpenseManager.buildSingleExpenseView(newIndex, authorId);
            e.getHook().editOriginalEmbeds(messageData.getEmbeds())
                    .setComponents(messageData.getComponents())
                    .queue();


        } else if (prefix.equals("settleup-explain")) {
            e.deferReply(true).queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.BLUE);
            embed.setTitle("How the Settlement is Calculated");

            embed.addField("Totaling the Bills 💵",
                    "First, I look at every single expense and add up the total amount of money each person spent. This shows who contributed money to the trip.",
                    false);

            embed.addField("Finding the 'Fair Share' ➗",
                    "Next, for each shared expense, I calculate a \"fair share.\" For example, if a $30 pizza was shared by 3 people, everyone's fair share of that pizza is $10.",
                    false);

            embed.addField("Checking the Balance 👍",
                    "Then, I compare how much you *spent* versus your total *fair share*.\n" +
                            "• If you spent **more** than your share, you are **owed money**.\n" +
                            "• If you spent **less** than your share, you **need to pay** money.",
                    false);

            embed.addField("Simplifying the Payments ➡️",
                    "So, instead of a messy web of payments, I figure out the simplest way to get everyone even. I tell people who they need to pay and exactly how much, minimizing the number of payments required until all debts are cleared.",
                    false);

            e.getHook().sendMessageEmbeds(embed.build()).queue();
        }
    }

}
