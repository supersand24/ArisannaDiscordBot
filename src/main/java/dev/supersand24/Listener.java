package dev.supersand24;

import java.util.List;
import java.util.stream.Collectors;

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
            List<Command.Choice> options = JsonCounterManager.getCounterNames().stream()
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
                    if (JsonCounterManager.getCounterNames().contains(optionName)) {
                        e.reply(optionName + " counter already exists!").setEphemeral(true).queue();
                    } else {
                        String optionDescription = e.getOption("description").getAsString();
                        int initialValue = e.getOption("initial-value") != null ? e.getOption("initial-value").getAsInt() : 0;
                        int minValue = e.getOption("min-value") != null ? e.getOption("min-value").getAsInt() : 0;
                        int maxValue = e.getOption("max-value") != null ? e.getOption("max-value").getAsInt() : Integer.MAX_VALUE;
                        JsonCounterManager.createCounter(optionName, optionDescription, initialValue, minValue, maxValue, commandUser.getId());
                        e.reply(optionName + " counter was created!").queue();
                    }
                    return;
                }

                String counterName = e.getOption("counter").getAsString();
                //Need to not expose counter here, need to update to strictly use the Manager.
                CounterData counter = JsonCounterManager.get(counterName);

                //Check to see if Counter exists
                if (!JsonCounterManager.getCounterNames().contains(counterName)) {
                    e.reply("I can't find a counter named " + counterName + "!").setEphemeral(true).queue();
                    return;
                }

                //Check to see if user has editing access
                if (!counter.allowedEditors.contains(commandUser.getId())) {
                    e.reply("You don't have editing access on " + counterName + " counter.").setEphemeral(true).queue();
                    return;
                }

                String subCommandGroup = e.getSubcommandGroup();

                if (subCommandGroup == null) {
                    switch (e.getSubcommandName()) {
                        case "increment" -> {
                            JsonCounterManager.increment(counterName);
                            e.reply(counterName + " counter incremented to " + counter.value + ".").queue();
                        }
                        case "decrement" -> {
                            JsonCounterManager.adjust(counterName, -1);
                            e.reply(counterName + " counter decremented to " + counter.value + ".").queue();
                        }
                        case "set" -> {
                            int value = e.getOption("value").getAsInt();
                            JsonCounterManager.set(counterName, value);
                            e.reply(counterName + " counter set to " + value + ".").queue();
                        }
                        case "display" -> e.replyEmbeds(JsonCounterManager.getCounterEmbed(counterName)).queue();
                        case "delete" -> {
                            JsonCounterManager.deleteCounter(counterName);
                            e.reply(counterName + " counter was deleted!").queue();
                        }
                    }
                } else {
                    switch (subCommandGroup) {
                        case "editor" -> {
                            User editor = e.getOption("editor").getAsUser();
    
                            switch (e.getSubcommandName()) {
                                case "add" -> {
                                    if (counter.allowedEditors.contains(editor.getId())) {
                                        e.reply(editor.getName() + " is already authorized on " + counter.name + " counter.").setEphemeral(true).queue();
                                    } else {
                                        counter.allowedEditors.add(editor.getId());
                                        e.reply(editor.getName() + " is now an editor of " + counter.name + " counter.").setEphemeral(true).queue();
                                        JsonCounterManager.markDirty();
                                    }
                                }
                                case "remove" -> {
                                    if (counter.allowedEditors.contains(editor.getId())) {
                                        counter.allowedEditors.remove(editor.getId());
                                        e.reply(editor.getName() + " is no longer an editor of " + counter.name + " counter.").setEphemeral(true).queue();
                                        JsonCounterManager.markDirty();
                                    } else {
                                        e.reply(editor.getName() + " is not currently authorized on " + counter.name + " counter.").setEphemeral(true).queue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
