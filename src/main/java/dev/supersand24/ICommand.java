package dev.supersand24;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface ICommand {

    String getName();

    CommandData getCommandData();

    default void handleSlashCommand(SlashCommandInteractionEvent e) {}

    default void handleButtonInteraction(ButtonInteractionEvent e) {}

    default void handleStringSelectInteraction(StringSelectInteractionEvent e) {}

    default void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {}

    default void handleModalInteraction(ModalInteractionEvent e) {}

}
