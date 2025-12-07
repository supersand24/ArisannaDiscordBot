package dev.supersand24.counters;

import dev.supersand24.ICommand;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class CounterCommand implements ICommand {

    @Override
    public String getName() { return "counter"; }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {

        User commandUser = e.getUser();

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
