package dev.supersand24.events;

import dev.supersand24.ICommand;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class RolesCommand implements ICommand {

    @Override
    public String getName() { return "roles"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("roles", "Commands for managing roles");
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        List<EventData> events = new ArrayList<>();
        long today = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        for (EventData event : EventManager.getAllEvents()) {
            if (event.getStartDate() > today)
                events.add(event);
        }

        List<ActionRowChildComponent> buttons = new ArrayList<>();
        for (EventData event : events) {
            buttons.add(Button.secondary("role-select:" + event.getId(), event.getName()));
        }

        e.reply("Sending Role Selection now!").setEphemeral(true).queue();

        Container container = Container.of(
                TextDisplay.of("## Role Selection"),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of("Select which events you will be attending using the buttons below.  Selecting an event you already have a role for will remove it."),
                ActionRow.of(buttons)
        );

        e.getChannel().sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.startsWith("role-select")) {
            String roleId = parts[1];
            Role role = EventManager.getRole(Long.parseLong(roleId));

            if (role == null) {
                e.reply("Sorry there was an issue, I can't find the role you need!").setEphemeral(true).queue();
                return;
            }

            Member member = e.getMember();
            Guild guild = e.getGuild();
            if (member == null || guild == null) return;

            e.deferReply(true).queue();

            if (member.getRoles().contains(role))
                guild.removeRoleFromMember(member, role).queue(unused -> e.getHook().sendMessage(role.getAsMention() + " was removed.").setSuppressedNotifications(true).setEphemeral(true).queue());
            else
                guild.addRoleToMember(member, role).queue(unused -> e.getHook().sendMessage(role.getAsMention() + " was added.").setSuppressedNotifications(true).setEphemeral(true).queue());
        }
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
