package dev.supersand24.expenses;

import dev.supersand24.CurrencyUtils;
import dev.supersand24.ICommand;
import dev.supersand24.Listener;
import dev.supersand24.events.EventData;
import dev.supersand24.events.EventManager;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExpenseCommand implements ICommand {

    private final Logger log = LoggerFactory.getLogger(ExpenseCommand.class);

    @Override
    public String getName() { return "expense"; }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        switch (e.getSubcommandName()) {
            case "add" -> {
                String optionName = e.getOption("name").getAsString();
                double optionAmount = e.getOption("amount").getAsDouble();

                //Temp
                long expenseId = ExpenseManager.createExpense(optionName, optionAmount, e.getUser().getId(), EventManager.getAllEvents().getFirst());

                e.replyComponents(Container.of(
                        TextDisplay.of("Created " + CurrencyUtils.formatAsUSD(optionAmount) + " expense."),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("Choose who benefited from " + optionName + "."),
                        ActionRow.of(EntitySelectMenu.create("expense-beneficiary-select:" + expenseId, EntitySelectMenu.SelectTarget.USER)
                                .setDefaultValues(EntitySelectMenu.DefaultValue.user(e.getUser().getId()))
                                .setMaxValues(20)
                                .build())
                )).useComponentsV2().queue();
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
                    if (sortedExpenses.get(i).getId() == expenseId) {
                        initialIndex = i;
                        break;
                    }
                }

                if (initialIndex == -1) {
                    e.getHook().sendMessage("This expense doesn't exist in my library!").setEphemeral(true).queue();
                    return;
                }

                MessageCreateData messageData = ExpenseManager.generateExpenseDetailMessage(e.getUser().getId(), initialIndex);
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
                e.getHook().sendMessageComponents(ExpenseManager.buildPaymentInfoDetailContainer())
                        .useComponentsV2()
                        .queue();
            }
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.startsWith("expense-")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            if (prefix.equals("expense-edit")) {

                int index = Integer.parseInt(parts[2]);
                e.editComponents(ExpenseManager.generateExpenseEditMessage(authorId, index).getComponents())
                        .useComponentsV2()
                        .queue();

            } else if (prefix.startsWith("expense-edit-")) {

                int index = Integer.parseInt(parts[2]);

                switch (prefix) {
                    case "expense-edit-name" -> e.replyModal(ExpenseManager.generateEditExpenseNameModal(index)).queue();
                    case "expense-edit-amount" -> e.replyModal(ExpenseManager.generateEditExpenseAmountModal(index)).queue();
                    case "expense-edit-event" -> e.replyModal(ExpenseManager.generateEditExpenseEventLinkedModal(index)).queue();
                    case "expense-edit-delete" -> {
                        Modal modal = ExpenseManager.generateDeleteExpenseModel(index);
                        if (modal == null)
                            e.reply("Could not delete non existing expense!").setEphemeral(true).queue();
                        else
                            e.replyModal(modal).queue();
                    }
                    case "expense-edit-view" -> e.editComponents(ExpenseManager.buildExpenseDetailContainer(index, authorId))
                            .useComponentsV2()
                            .queue();
                    case "expense-edit-view-list" ->
                            e.editComponents(ExpenseManager.buildExpenseListContainer(ExpenseManager.getExpensesSorted(), 0, authorId))
                                    .useComponentsV2()
                                    .queue();
                    default -> {
                        log.error("Unexpected Expense Edit Button Pressed!");
                        e.reply("Something went wrong!").setEphemeral(true).queue();
                    }
                }

            }
            else {

                e.deferEdit().useComponentsV2().queue();

                MessageCreateData data = new MessageCreateBuilder().setContent("No events.").build();

                switch (prefix) {
                    case "expense-list-prev", "expense-list-next" -> {
                        int currentPage = Integer.parseInt(parts[2]);
                        int newPage = prefix.equals("expense-list-next") ? currentPage + 1 : currentPage - 1;
                        data = ExpenseManager.generateExpenseListMessage(authorId, newPage);
                    }
                    case "expense-list-zoom" -> {
                        int index = Integer.parseInt(parts[2]);
                        data = ExpenseManager.generateExpenseDetailMessage(authorId, index);
                    }
                    case "expense-detail-back" -> {
                        data = ExpenseManager.generateExpenseListMessage(authorId, 0);
                    }
                }

                e.getHook().editOriginalComponents(data.getComponents())
                        .useComponentsV2()
                        .queue();
            }

        }
    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];
        String authorId = parts.length > 1 ? parts[1] : "";

        if (prefix.equals("expense-list-zoom")) {
            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            int index = Integer.parseInt(e.getValues().get(0));
            e.editComponents(ExpenseManager.buildExpenseDetailContainer(index, authorId))
                    .useComponentsV2()
                    .queue();
        }
    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.equals("expense-beneficiary-select")) {
            long expenseId = Long.parseLong(parts[1]);

            if (!ExpenseManager.exists(expenseId)) {
                log.error("Could not find expense: " + expenseId);
                e.reply("This expense doesn't exist in my library!").setEphemeral(true).queue();
                return;
            }

            List<User> beneficiaries = e.getMentions().getUsers();

            ExpenseManager.addBenefactors(expenseId, beneficiaries);

            if (beneficiaries.size() == 1) {
                e.reply("Added 1 person to " + ExpenseManager.getExpenseName(expenseId) + " expense.").setEphemeral(true).queue();
            } else {
                e.reply("Added " + beneficiaries.size() + " people to " + ExpenseManager.getExpenseName(expenseId) + " expense.").setEphemeral(true).queue();
            }
        } else if (prefix.equals("expense-edit-payer")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            int index = Integer.parseInt(parts[2]);
            ExpenseManager.setExpensePayer(index, e.getMentions().getUsers().getFirst().getId());
            e.reply("Payer Updated").setEphemeral(true).queue();
        } else if (prefix.equals("expense-edit-beneficiary")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            int index = Integer.parseInt(parts[2]);
            ExpenseManager.addBenefactors(index, e.getMentions().getUsers());
            e.reply("Benefactors Updated").setEphemeral(true).queue();
        }
    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {
        String[] parts = e.getModalId().split(":");
        String prefix = parts[0];

        if (prefix.startsWith("expense-edit-")) {
            long expenseIndex = Long.parseLong(parts[1]);

            switch (prefix) {
                case "expense-edit-name" -> {
                    boolean hasChanged = false;
                    ModalMapping name = e.getValue("name");
                    if (name != null) {
                        ExpenseManager.setExpenseName(expenseIndex, name.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Name updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "expense-edit-amount" -> {
                    ModalMapping name = e.getValue("amount");
                    if (name != null) {
                        try {
                            double amount = Double.parseDouble(name.getAsString());
                            ExpenseManager.setExpenseAmount(expenseIndex, amount);
                            e.reply("Amount updated successfully!").setEphemeral(true).queue();
                        } catch (NumberFormatException ex) {
                            e.reply("Invalid format. Please enter a valid number (e.g., 12.34).").setEphemeral(true).queue();
                        }
                    }
                }
                case "expense-edit-event" -> {
                    ModalMapping name = e.getValue("event");
                    if (name != null) {
                        String eventName = name.getAsString();
                        EventData event = null;

                        for (EventData eve : EventManager.getAllEvents()) {
                            if (eventName.equals(eve.getName())) {
                                event = eve;
                                System.out.println("Cound VEvent");
                            }
                        }

                        if (event == null) {
                            e.reply("I could not find that event name.").setEphemeral(true).queue();
                        }
                        else
                        {
                            ExpenseManager.setExpenseLinkedEvent(expenseIndex, event);
                            e.reply("Linked Event updated successfully!").setEphemeral(true).queue();
                        }
                    }
                }
                case "expense-edit-delete" -> {
                    ModalMapping name = e.getValue("name");
                    if (name == null) { e.reply("There was an issue deleting the expense!").setEphemeral(true).queue(); return; }
                    if (name.getAsString().equals(ExpenseManager.getExpenseName(expenseIndex))) {
                        if (EventManager.deleteEvent(expenseIndex))
                            e.reply(name.getAsString() + " was deleted!").setEphemeral(true).queue();
                        else
                            e.reply("There was an issue deleting the expense!").setEphemeral(true).queue();
                    }
                    else e.reply("That is not the correct expense name!").setEphemeral(true).queue();
                }
            }
        }
    }

}
