package dev.supersand24;

import java.nio.channels.Channel;
import java.util.List;
import java.util.stream.Collectors;

import dev.supersand24.counters.CounterManager;
import dev.supersand24.expenses.ExpenseManager;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
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
                String optionName = e.getOption("name").getAsString();
                double optionAmount = e.getOption("amount").getAsDouble();

                String expenseId = ExpenseManager.createExpense(optionName, optionAmount, e.getUser().getId());

                e.reply("Created " + CurrencyUtils.formatAsUSD(optionAmount) + " expense.\nChoose who benefited from " + optionName + ".")
                        .addActionRow(
                                EntitySelectMenu.create("expense-beneficiary-select:" + expenseId, EntitySelectMenu.SelectTarget.USER)
                                        .setMaxValues(20)
                                        .build()
                        ).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent e) {
        if (e.getComponentId().startsWith(ExpenseManager.getInteractionPrefix())) {
            String expenseId = e.getComponentId().substring(ExpenseManager.getInteractionPrefix().length());

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

}
