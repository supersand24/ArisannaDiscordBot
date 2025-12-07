package dev.supersand24;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface ICommand {

    String getName();

    void handleSlashCommand(SlashCommandInteractionEvent e);

    void handleButtonInteraction(ButtonInteractionEvent e);

    void handleStringSelectInteraction(StringSelectInteractionEvent e);

    void handleEntitySelectInteraction(EntitySelectInteractionEvent e);

    void handleModalInteraction(ModalInteractionEvent e);

}
