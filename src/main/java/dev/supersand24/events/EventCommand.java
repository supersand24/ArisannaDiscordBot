package dev.supersand24.events;

import dev.supersand24.ArisannaBot;
import dev.supersand24.DataStore;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class EventCommand implements ICommand {

    private final Logger log = LoggerFactory.getLogger(EventCommand.class);

    @Override
    public String getName() { return "event"; }

    @Override
    public CommandData getCommandData() {
         return Commands.slash("event", "Manage travel events.")
                .addSubcommands(
                        new SubcommandData("create", "Create a new event.")
                                .addOption(OptionType.STRING, "name", "The name of the event.", true),
                        new SubcommandData("list", "List all created events."),
                        new SubcommandData("gauge-interest", "List all events we are currently gauging for interest."),
                        new SubcommandData("edit", "Edit the details of an existing event.")
                                .addOption(OptionType.INTEGER, "id", "The ID of the event to edit.", true)
                                .addOption(OptionType.STRING, "name", "The new name for the event.", false)
                                .addOption(OptionType.STRING, "start-date", "The event's start date (e.g., 03/27/2025).", false)
                                .addOption(OptionType.STRING, "end-date", "The event's end date (e.g., 03/30/2025).", false)
                                .addOption(OptionType.ROLE, "role", "The role associated with this event.", false)
                                .addOption(OptionType.CHANNEL, "channel", "The channel for event discussions.", false)
                                .addOption(OptionType.STRING, "address", "The physical address of the event venue.", false)
                                .addOption(OptionType.STRING, "omnidex-link", "The event's omnidex link.", false)
                                .addOption(OptionType.STRING, "ticket-link", "The event's ticket link.", false)
                )
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_EVENTS));
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        switch (e.getSubcommandName()) {
            case "create" -> {
                String eventName = e.getOption("name").getAsString();
                EventManager.createEvent(guildId, eventName);
                e.reply("Created new event: **" + eventName + "**").setEphemeral(true).queue();
            }
            case "list" -> {
                e.deferReply().queue();
                MessageCreateData messageData = EventManager.generateListMessage(guildId, e.getUser().getId(), 0);
                e.getHook().sendMessage(messageData).useComponentsV2().queue();
            }
            case "gauge-interest" -> {
                e.deferReply().queue();
                MessageCreateData messageData = EventManager.generateGaugeInterestList(guildId, 0);
                e.getHook().sendMessage(messageData).useComponentsV2().queue();
            }
            case "edit" -> {
                long eventId = e.getOption("id").getAsLong();

                StringBuilder response = new StringBuilder("## Updated Event #" + eventId + "\n");
                boolean changed = false;

                OptionMapping nameOpt = e.getOption("name");
                if (nameOpt != null) {
                    String newName = nameOpt.getAsString();
                    EventManager.setEventName(guildId, eventId, newName);
                    response.append("- Name set to: **").append(newName).append("**\n");
                    changed = true;
                }

                OptionMapping startDateOpt = e.getOption("start-date");
                if (startDateOpt != null) {
                    long timestamp = parseDateToEpochMilli(startDateOpt.getAsString());
                    if (timestamp == -1) {
                        e.reply("Invalid start date format. Please use `MM/DD/YYYY`.").setEphemeral(true).queue();
                        return;
                    }
                    EventManager.setStartDate(guildId, eventId, timestamp);
                    response.append("- Start date set to: <t:").append(timestamp / 1000).append(":D>\n");
                    changed = true;
                }

                OptionMapping endDateOpt = e.getOption("end-date");
                if (endDateOpt != null) {
                    long timestamp = parseDateToEpochMilli(endDateOpt.getAsString());
                    if (timestamp == -1) {
                        e.reply("Invalid end date format. Please use `MM/DD/YYYY`.").setEphemeral(true).queue();
                        return;
                    }
                    EventManager.setEndDate(guildId, eventId, timestamp);
                    response.append("- End date set to: <t:").append(timestamp / 1000).append(":D>\n");
                    changed = true;
                }

                OptionMapping roleOpt = e.getOption("role");
                if (roleOpt != null) {
                    Role role = roleOpt.getAsRole();
                    EventManager.setRoleId(guildId, eventId, role.getIdLong());
                    response.append("- Role set to: ").append(role.getAsMention()).append("\n");
                    changed = true;
                }

                OptionMapping channelOpt = e.getOption("channel");
                if (channelOpt != null) {
                    GuildChannel channel = channelOpt.getAsChannel();
                    EventManager.setChannelId(guildId, eventId, channel.getIdLong());
                    response.append("- Channel set to: ").append(channel.getAsMention()).append("\n");
                    changed = true;
                }

                OptionMapping addressOpt = e.getOption("address");
                if (addressOpt != null) {
                    String address = addressOpt.getAsString();
                    EventManager.setAddress(guildId, eventId, address);
                    response.append("- Address set to: `").append(address).append("`\n");
                    changed = true;
                }

                OptionMapping omnidexLinkOpt = e.getOption("omnidex-link");
                if (omnidexLinkOpt != null) {
                    String omnidexLink = omnidexLinkOpt.getAsString();
                    EventManager.setOmnidexLink(guildId, eventId, omnidexLink);
                    response.append("- Omnidex Link set to: ").append(omnidexLink).append("\n");
                    changed = true;
                }

                OptionMapping ticketLinkOpt = e.getOption("ticket-link");
                if (ticketLinkOpt != null) {
                    String ticketLink = ticketLinkOpt.getAsString();
                    EventManager.setTicketLink(guildId, eventId, ticketLink);
                    response.append("- Ticket Link set to: ").append(ticketLink).append("\n");
                    changed = true;
                }

                if (changed) {
                    DataStore.markDirty(guildId, "events");
                    e.reply(response.toString()).setEphemeral(true).queue();
                } else {
                    e.reply("No changes were provided.").setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[1];

        log.info("Processing {} button interaction.", prefix);

        String authorId = parts[2];

        if (!authorId.isEmpty()) {
            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }
        }

        if (prefix.equals("edit")) {

            int index = Integer.parseInt(parts[3]);

            e.editComponents(EventManager.buildEditContainer(guildId, index, authorId))
                    .useComponentsV2()
                    .queue();

        }
        else if (prefix.startsWith("edit-")) {

            int index = Integer.parseInt(parts[3]);

            switch (prefix) {
                case "edit-name" -> e.replyModal(EventManager.generateEditNameModal(guildId, index)).queue();
                case "edit-dates" -> e.replyModal(EventManager.generateEditDatesModal(guildId, index)).queue();
                case "edit-address" -> e.replyModal(EventManager.generateEditAddressModal(guildId, index)).queue();
                case "edit-omnidex" -> e.replyModal(EventManager.generateEditOmnidexModal(guildId, index)).queue();
                case "edit-ticket" -> e.replyModal(EventManager.generateEditTicketLinkModal(guildId, index)).queue();
                case "edit-gaugeInterest" -> {
                    boolean isGaugeInterest = EventManager.isGaugeInterest(guildId, index);
                    EventManager.setGaugeInterest(guildId, index, !isGaugeInterest);
                    e.getMessage().editMessageComponents(EventManager.buildEditContainer(guildId, index, authorId)).useComponentsV2().queue();
                    e.reply(isGaugeInterest ? "Event updated to stop gauging interest!" : "Event updated to start start gauging interest!").setEphemeral(true).queue();
                }
                case "edit-interestedMembers" -> {
                    StringBuilder sb = new StringBuilder();
                    for (Long l : EventManager.getMembersInterested(guildId, index)) {
                        sb.append(e.getGuild().getMemberById(l).getAsMention()).append("\n");
                    }
                    e.reply(sb.toString()).setSuppressedNotifications(true).setEphemeral(true).queue();
                }
                case "edit-delete" -> {
                    Modal modal = EventManager.generateDeleteEventModel(guildId, index);
                    if (modal == null)
                        e.reply("Could not delete non existing event!").setEphemeral(true).queue();
                    else
                        e.replyModal(modal).queue();
                }
                case "edit-view" -> e.editComponents(EventManager.buildDetailContainer(guildId, index, authorId))
                        .useComponentsV2()
                        .queue();
                case "edit-view-list" ->
                        e.editComponents(EventManager.buildListContainer(EventManager.getAllEvents(guildId), 0, authorId))
                                .useComponentsV2()
                                .queue();
                default -> {
                    log.error("Unexpected Event Edit Button Pressed!");
                    e.reply("Something went wrong!").setEphemeral(true).queue();
                }
            }

        }
        else if (prefix.equals("gaugeInterest")) {

            int eventIndex = Integer.parseInt(parts[3]);
            Member member = e.getMember();

            if (EventManager.isMemberInterested(guildId, eventIndex, member)) {
                EventManager.removeInterestedMember(guildId, eventIndex, member);
                e.reply("You have been marked down as **uninterested**.").setEphemeral(true).queue();
            } else {
                EventManager.addInterestedMember(guildId, eventIndex, member);
                e.reply("You have been marked down as **interested**.").setEphemeral(true).queue();
            }

            e.getMessage().editMessageComponents(EventManager.buildGaugeInterestListContainer(EventManager.getAllEventsGaugingInterest(guildId), 1)).useComponentsV2().queue();
        }
        else {

            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No events.").build();

            switch (prefix) {
                case "list-prev", "list-next" -> {
                    int currentPage = Integer.parseInt(parts[3]);
                    int newPage = prefix.equals("list-next") ? currentPage + 1 : currentPage - 1;
                    data = EventManager.generateListMessage(guildId, authorId, newPage);
                }
                case "list-zoom" -> {
                    int index = Integer.parseInt(parts[3]);
                    data = EventManager.generateDetailMessage(guildId, authorId, index);
                }
                case "detail-prev", "detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[3]);
                    int newIndex = prefix.equals("detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = EventManager.generateDetailMessage(guildId, authorId, newIndex);
                }
                case "detail-back" -> data = EventManager.generateListMessage(guildId, authorId, 0);
            }

            e.getHook().editOriginalComponents(data.getComponents())
                    .useComponentsV2()
                    .queue();
        }


    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[1];
        String authorId = parts.length > 2 ? parts[2] : "";

        if (prefix.equals("list-zoom")) {
            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            int index = Integer.parseInt(e.getValues().getFirst());
            MessageCreateData data = EventManager.generateDetailMessage(guildId, authorId, index);
            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }
    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {

        if (e.getGuild() == null) return;

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[1];
        long authorId = ArisannaBot.parseLongSafe(parts, 2);
        long eventId = ArisannaBot.parseLongSafe(parts, 3);
        String guildId = e.getGuild().getId();

        log.info("Received!  " + prefix + " - " + authorId);

        if (prefix.equals("edit-channel")) {
            boolean hasChanged = false;
            Channel channel = e.getMentions().getChannels().getFirst();
            if (channel != null) {
                EventManager.setChannelId(guildId, eventId, channel.getIdLong());
                hasChanged = true;
            }
            e.reply(hasChanged ? "Channel set to " + channel.getAsMention() + " successfully!" : "No changes to channel were made.").setEphemeral(true).queue();
        } else if (prefix.equals("edit-role")) {
            List<Role> mentionedRoles = e.getMentions().getRoles();
            Role role = mentionedRoles.isEmpty() ? null : mentionedRoles.getFirst();
            Role currentRole = EventManager.getRole(guildId, eventId);
            String replyMsg;

            if (role != null) {
                if (currentRole == null || currentRole.equals(role)) {
                    EventManager.setRoleId(guildId, eventId, role.getIdLong());
                    replyMsg = "Role set to " + role.getAsMention() + " successfully!";
                } else {
                    replyMsg = "No changes to role were made.";
                }
            } else {
                if (currentRole != null) {
                    EventManager.setRoleId(guildId, eventId, 0L);
                    replyMsg = "Role was successfully unselected!";
                } else {
                    replyMsg = "No changes to role were made.";
                }
            }

            e.reply(replyMsg)
                    .setEphemeral(true)
                    .setSuppressedNotifications(true)
                    .queue();
        }
    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        String[] parts = e.getModalId().split(":");
        String prefix = parts[1];

        if (prefix.startsWith("edit-")) {
            long eventIndex = Long.parseLong(parts[2]);

            switch (prefix) {
                case "edit-name" -> {
                    boolean hasChanged = false;
                    ModalMapping name = e.getValue("name");
                    if (name != null) {
                        EventManager.setEventName(guildId, eventIndex, name.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Name updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-dates" -> {

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                    boolean hasChanged = false;

                    ModalMapping startDateMapping = e.getValue("start-date");
                    Long newStartDate = parseDateFromModal(startDateMapping, formatter);

                    if (newStartDate == null && startDateMapping != null && !startDateMapping.getAsString().isEmpty()) {
                        e.reply("Invalid start date format. Please use **MM/DD/YYYY**.").setEphemeral(true).queue();
                        return;
                    }

                    if (newStartDate != null) {
                        EventManager.setStartDate(guildId, eventIndex, newStartDate);
                        hasChanged = true;
                    }

                    ModalMapping endDateMapping = e.getValue("end-date");
                    Long newEndDate = parseDateFromModal(endDateMapping, formatter);

                    if (newEndDate == null && endDateMapping != null && !endDateMapping.getAsString().isEmpty()) {
                        e.reply("Invalid end date format. Please use **MM/DD/YYYY**.").setEphemeral(true).queue();
                        return;
                    }

                    if (newEndDate != null) {
                        EventManager.setEndDate(guildId, eventIndex, newEndDate);
                        hasChanged = true;
                    }

                    e.reply(hasChanged ? "Dates updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-address" -> {
                    boolean hasChanged = false;
                    ModalMapping address = e.getValue("address");
                    if (address != null) {
                        EventManager.setAddress(guildId, eventIndex, address.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Address updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-omnidex" -> {
                    boolean hasChanged = false;
                    ModalMapping omnidex = e.getValue("omnidex");
                    if (omnidex != null) {
                        EventManager.setOmnidexLink(guildId, eventIndex, omnidex.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Omnidex updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-ticket" -> {
                    boolean hasChanged = false;
                    ModalMapping ticketLink = e.getValue("ticket");
                    if (ticketLink != null) {
                        EventManager.setTicketLink(guildId, eventIndex, ticketLink.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Ticket Link updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-delete" -> {
                    ModalMapping name = e.getValue("name");
                    if (name == null) { e.reply("There was an issue deleting the event!").setEphemeral(true).queue(); return; }
                    String nameToDelete = EventManager.getEventName(guildId, eventIndex).isBlank() ? "Unnamed Event" : EventManager.getEventName(guildId, eventIndex);
                    if (name.getAsString().equals(nameToDelete)) {
                        if (EventManager.deleteEvent(guildId, eventIndex))
                            e.reply(name.getAsString() + " was deleted!").setEphemeral(true).queue();
                        else
                            e.reply("There was an issue deleting the event!").setEphemeral(true).queue();
                    }
                    else e.reply("That is not the correct event name!").setEphemeral(true).queue();
                }
            }
        }
    }

    private Long parseDateFromModal(ModalMapping mapping, DateTimeFormatter formatter) {
        if (mapping == null || mapping.getAsString().isEmpty()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(mapping.getAsString(), formatter);
            ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
            return zonedDateTime.toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            log.error("Invalid Date was entered. {}", mapping.getAsString());
            return null;
        }
    }

    private long parseDateToEpochMilli(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, formatter);
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }
}
