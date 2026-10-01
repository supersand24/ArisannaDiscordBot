package dev.supersand24.counters;

import dev.supersand24.ICommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;

public class CounterCommand implements ICommand {

    @Override
    public String getName() { return "counter"; }

    public CommandData getCommandData() {
        return Commands.slash(getName(), "Modifies Counters.")
                .addSubcommandGroups(
                        new SubcommandGroupData("editor", "Modifies Editors on a Counter.")
                                .addSubcommands(
                                        new SubcommandData("add", "Adds a new editor to a Counter.")
                                                .addOptions(
                                                        new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                                                        new OptionData(OptionType.USER, "editor", "Who to add as an Editor.").setRequired(true)
                                                ),
                                        new SubcommandData("remove", "Removes an editor from a Counter.")
                                                .addOptions(
                                                        new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                                                        new OptionData(OptionType.USER, "editor", "Who to remove as an Editor.").setRequired(true)
                                                )
                                )
                )
                .addSubcommands(
                        new SubcommandData("increment", "Increments a Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true)
                                ),
                        new SubcommandData("decrement", "Decrements a Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true)
                                ),
                        new SubcommandData("set", "Sets the Value of a Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                                        new OptionData(OptionType.INTEGER, "value", "New value of the Counter.", true)
                                ),
                        new SubcommandData("display", "Displays an existing Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "counter", "Counter to Display.", true, true)
                                ),
                        new SubcommandData("create", "Creates a new Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "name", "Name of the Counter.", true),
                                        new OptionData(OptionType.STRING, "description", "Description of the Counter.", true),
                                        new OptionData(OptionType.INTEGER, "initial-value", "Starting Value."),
                                        new OptionData(OptionType.INTEGER, "min-value", "Minimum Value."),
                                        new OptionData(OptionType.INTEGER, "max-value", "Maximum Value.")
                                ),
                        new SubcommandData("delete", "Deletes an existing Counter.")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "counter", "Counter to Delete.", true, true)
                                )
                );
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {

        if (e.getGuild() == null){
            e.reply("This command can only be used inside a server!").setEphemeral(true).queue();
            return;
        }

        String guildId = e.getGuild().getId();
        User commandUser = e.getUser();

        //If trying to create a Counter, handle first
        if (e.getFullCommandName().equals("counter create")) {
            String optionName = e.getOption("name").getAsString();
            if (CounterManager.getCounterNames(guildId).contains(optionName)) {
                e.reply(optionName + " counter already exists!").setEphemeral(true).queue();
            } else {
                String optionDescription = e.getOption("description").getAsString();
                int initialValue = e.getOption("initial-value") != null ? e.getOption("initial-value").getAsInt() : 0;
                int minValue = e.getOption("min-value") != null ? e.getOption("min-value").getAsInt() : 0;
                int maxValue = e.getOption("max-value") != null ? e.getOption("max-value").getAsInt() : Integer.MAX_VALUE;
                CounterManager.createCounter(guildId, optionName, optionDescription, initialValue, minValue, maxValue, commandUser.getId());
                e.reply(optionName + " counter was created!").queue();
            }
            return;
        }

        String counterName = e.getOption("counter").getAsString();

        //Check to see if Counter exists
        if (!CounterManager.getCounterNames(e.getGuild().getId()).contains(counterName)) {
            e.reply("I can't find a counter named " + counterName + "!").setEphemeral(true).queue();
            return;
        }

        //Check to see if user has editing access
        if (!CounterManager.canEdit(guildId, counterName, commandUser.getId())) {
            e.reply("You don't have editing access on " + counterName + " counter.").setEphemeral(true).queue();
            return;
        }

        //Different Command Groups
        switch (e.getSubcommandGroup()) {
            case null -> {
                switch (e.getSubcommandName()) {
                    case "increment" -> {
                        CounterManager.increment(guildId, counterName);
                        e.reply(counterName + " counter incremented to " + CounterManager.getValue(guildId, counterName) + ".").queue();
                    }
                    case "decrement" -> {
                        CounterManager.decrement(guildId, counterName);
                        e.reply(counterName + " counter decremented to " + CounterManager.getValue(guildId, counterName) + ".").queue();
                    }
                    case "set" -> {
                        int value = e.getOption("value").getAsInt();
                        CounterManager.setValue(guildId, counterName, value);
                        e.reply(counterName + " counter set to " + value + ".").queue();
                    }
                    case "display" -> e.replyEmbeds(CounterManager.getCounterEmbed(guildId, counterName)).queue();
                    case "delete" -> {
                        CounterManager.deleteCounter(guildId, counterName);
                        e.reply(counterName + " counter was deleted!").queue();
                    }
                }
            }
            case "editor" -> {
                User editor = e.getOption("editor").getAsUser();

                switch (e.getSubcommandName()) {
                    case "add" -> {
                        if (CounterManager.canEdit(guildId, counterName, editor.getId())) {
                            e.reply(editor.getName() + " is already authorized on " + counterName + " counter.").setEphemeral(true).queue();
                        } else {
                            CounterManager.addEditor(guildId, counterName, editor.getId());
                            e.reply(editor.getName() + " is now an editor of " + counterName + " counter.").setEphemeral(true).queue();
                        }
                    }
                    case "remove" -> {
                        if (CounterManager.canEdit(guildId, counterName, editor.getId())) {
                            CounterManager.removeEditor(guildId, counterName, editor.getId());
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

}
