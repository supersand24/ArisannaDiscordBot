package dev.supersand24.events;

import dev.supersand24.DataStore;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
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
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
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

public class EventCommand implements ICommand {

    private final Logger log = LoggerFactory.getLogger(EventCommand.class);

    @Override
    public String getName() { return "event"; }

    @Override
    public CommandData getCommandData() {
         return Commands.slash("event", "Manage travel events.")
                .addSubcommands(
                        new SubcommandData("create", "Create a new event.")
                                .addOption(OptionType.STRING, "name", "The name of the new event", true),
                        new SubcommandData("list", "List all created events."),
                        new SubcommandData("edit", "Edit the details of an existing event.")
                                .addOption(OptionType.INTEGER, "id", "The ID of the event to edit.", true)
                                .addOption(OptionType.STRING, "name", "The new name for the event.", false)
                                .addOption(OptionType.STRING, "start-date", "The event's start date (e.g., 03/27/2025).", false)
                                .addOption(OptionType.STRING, "end-date", "The event's end date (e.g., 03/30/2025).", false)
                                .addOption(OptionType.ROLE, "role", "The role associated with this event.", false)
                                .addOption(OptionType.CHANNEL, "channel", "The channel for event discussions.", false)
                                .addOption(OptionType.STRING, "address", "The physical address of the event venue.", false)
                                .addOption(OptionType.STRING, "omnidex-link", "The event's omnidex link.", false)
                )
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_EVENTS));
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        switch (e.getSubcommandName()) {
            case "create" -> {
                String eventName = e.getOption("name").getAsString();
                EventManager.createEvent(eventName);
                e.reply("Created new event: **" + eventName + "**").setEphemeral(true).queue();
            }
            case "list" -> {
                e.deferReply().queue();
                MessageCreateData messageData = EventManager.generateListMessage(e.getUser().getId(), 0);
                e.getHook().sendMessage(messageData).useComponentsV2().queue();
            }
            case "edit" -> {
                long eventId = e.getOption("id").getAsLong();

                StringBuilder response = new StringBuilder("## Updated Event #" + eventId + "\n");
                boolean changed = false;

                OptionMapping nameOpt = e.getOption("name");
                if (nameOpt != null) {
                    String newName = nameOpt.getAsString();
                    EventManager.setEventName(eventId, newName);
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
                    EventManager.setStartDate(eventId, timestamp);
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
                    EventManager.setEndDate(eventId, timestamp);
                    response.append("- End date set to: <t:").append(timestamp / 1000).append(":D>\n");
                    changed = true;
                }

                OptionMapping roleOpt = e.getOption("role");
                if (roleOpt != null) {
                    Role role = roleOpt.getAsRole();
                    EventManager.setRoleId(eventId, role.getIdLong());
                    response.append("- Role set to: ").append(role.getAsMention()).append("\n");
                    changed = true;
                }

                OptionMapping channelOpt = e.getOption("channel");
                if (channelOpt != null) {
                    GuildChannel channel = channelOpt.getAsChannel();
                    EventManager.setChannelId(eventId, channel.getIdLong());
                    response.append("- Channel set to: ").append(channel.getAsMention()).append("\n");
                    changed = true;
                }

                OptionMapping addressOpt = e.getOption("address");
                if (addressOpt != null) {
                    String address = addressOpt.getAsString();
                    EventManager.setAddress(eventId, address);
                    response.append("- Address set to: `").append(address).append("`\n");
                    changed = true;
                }

                OptionMapping linkOpt = e.getOption("omnidex");
                if (linkOpt != null) {
                    String link = linkOpt.getAsString();
                    EventManager.setOmnidexLink(eventId, link);
                    response.append("- Link set to: ").append(link).append("\n");
                    changed = true;
                }

                if (changed) {
                    DataStore.markDirty("events");
                    e.reply(response.toString()).setEphemeral(true).queue();
                } else {
                    e.reply("No changes were provided.").setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[1];

        log.info("Processing " + prefix + " button interaction.");

        String authorId = parts[2];

        if (!e.getUser().getId().equals(authorId)) {
            e.reply("You cannot use these buttons.").setEphemeral(true).queue();
            return;
        }

        if (prefix.equals("edit")) {

            int index = Integer.parseInt(parts[3]);

            e.editComponents(EventManager.generateEditContainer(index, authorId))
                    .useComponentsV2()
                    .queue();

        }
        else if (prefix.startsWith("edit-")) {

            int index = Integer.parseInt(parts[3]);

            switch (prefix) {
                case "edit-name" -> e.replyModal(EventManager.generateEditNameModal(index)).queue();
                case "edit-dates" -> e.replyModal(EventManager.generateEditDatesModal(index)).queue();
                case "edit-address" -> e.replyModal(EventManager.generateEditAddressModal(index)).queue();
                case "edit-omnidex" -> e.replyModal(EventManager.generateEditOmnidexModal(index)).queue();
                case "edit-delete" -> {
                    Modal modal = EventManager.generateDeleteEventModel(index);
                    if (modal == null)
                        e.reply("Could not delete non existing event!").setEphemeral(true).queue();
                    else
                        e.replyModal(modal).queue();
                }
                case "edit-view" -> e.editComponents(EventManager.buildDetailContainer(index, authorId))
                        .useComponentsV2()
                        .queue();
                case "edit-view-list" ->
                        e.editComponents(EventManager.buildListContainer(EventManager.getAllEvents(), 0, authorId))
                                .useComponentsV2()
                                .queue();
                default -> {
                    log.error("Unexpected Event Edit Button Pressed!");
                    e.reply("Something went wrong!").setEphemeral(true).queue();
                }
            }

        }
        else {

            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No events.").build();

            switch (prefix) {
                case "list-prev", "list-next" -> {
                    int currentPage = Integer.parseInt(parts[3]);
                    int newPage = prefix.equals("list-next") ? currentPage + 1 : currentPage - 1;
                    data = EventManager.generateListMessage(authorId, newPage);
                }
                case "list-zoom" -> {
                    int index = Integer.parseInt(parts[3]);
                    data = EventManager.generateDetailMessage(authorId, index);
                }
                case "detail-prev", "detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[3]);
                    int newIndex = prefix.equals("detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = EventManager.generateDetailMessage(authorId, newIndex);
                }
                case "detail-back" -> data = EventManager.generateListMessage(authorId, 0);
            }

            e.getHook().editOriginalComponents(data.getComponents())
                    .useComponentsV2()
                    .queue();
        }


    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {
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
            MessageCreateData data = EventManager.generateDetailMessage(authorId, index);
            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }
    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {

    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {
        String[] parts = e.getModalId().split(":");
        String prefix = parts[1];

        if (prefix.startsWith("edit-")) {
            long eventIndex = Long.parseLong(parts[2]);

            switch (prefix) {
                case "edit-name" -> {
                    boolean hasChanged = false;
                    ModalMapping name = e.getValue("name");
                    if (name != null) {
                        EventManager.setEventName(eventIndex, name.getAsString());
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
                        EventManager.setStartDate(eventIndex, newStartDate);
                        hasChanged = true;
                    }

                    ModalMapping endDateMapping = e.getValue("end-date");
                    Long newEndDate = parseDateFromModal(endDateMapping, formatter);

                    if (newEndDate == null && endDateMapping != null && !endDateMapping.getAsString().isEmpty()) {
                        e.reply("Invalid end date format. Please use **MM/DD/YYYY**.").setEphemeral(true).queue();
                        return;
                    }

                    if (newEndDate != null) {
                        EventManager.setEndDate(eventIndex, newEndDate);
                        hasChanged = true;
                    }

                    e.reply(hasChanged ? "Dates updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-address" -> {
                    boolean hasChanged = false;
                    ModalMapping address = e.getValue("address");
                    if (address != null) {
                        EventManager.setAddress(eventIndex, address.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Address updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-omnidex" -> {
                    boolean hasChanged = false;
                    ModalMapping omnidex = e.getValue("omnidex");
                    if (omnidex != null) {
                        EventManager.setOmnidexLink(eventIndex, omnidex.getAsString());
                        hasChanged = true;
                    }
                    e.reply(hasChanged ? "Omnidex updated successfully!" : "No changes were made.").setEphemeral(true).queue();
                }
                case "edit-delete" -> {
                    ModalMapping name = e.getValue("name");
                    if (name == null) { e.reply("There was an issue deleting the event!").setEphemeral(true).queue(); return; }
                    if (name.getAsString().equals(EventManager.getEventName(eventIndex))) {
                        if (EventManager.deleteEvent(eventIndex))
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
