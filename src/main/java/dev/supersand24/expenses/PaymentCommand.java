package dev.supersand24.expenses;

import dev.supersand24.CurrencyUtils;
import dev.supersand24.ICommand;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;

public class PaymentCommand implements ICommand {

    @Override
    public String getName() { return "payment"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("payment", "Manage your payment information.")
                .addSubcommands(
                        new SubcommandData("add", "Add one of your payment methods (e.g., PayPal, Venmo).")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "app", "The name of the app (e.g., PayPal).", true)
                                                .addChoice("PayPal", "PayPal")
                                                .addChoice("Venmo", "Venmo")
                                                .addChoice("Cash App", "Cash App")
                                                .addChoice("Zelle", "Zelle"),
                                        new OptionData(OptionType.STRING, "details", "Your username, email, or phone number for the app.", true)
                                ),
                        new SubcommandData("remove", "Remove one of your payment methods.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "app", "The name of the app to remove (e.g., PayPal).", true)
                                                .addChoice("PayPal", "PayPal")
                                                .addChoice("Venmo", "Venmo")
                                                .addChoice("Cash App", "Cash App")
                                                .addChoice("Zelle", "Zelle")
                                ),
                        new SubcommandData("view", "View a user's saved payment methods.")
                                .addOption(OptionType.USER, "user", "The user whose payment info you want to see.", true)
                );
    }

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

    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {

    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {

    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {

    }

}
