package dev.supersand24.events;

import dev.supersand24.ArisannaBot;
import dev.supersand24.DataPartition;
import dev.supersand24.DataStore;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private static final String DATA_STORE_NAME = "events";

    /**
     * Creates a new event and saves it to the data store.
     *
     * @param name The name of the new event (e.g., "PAX East 2025").
     */
    public static long createEvent(String guildId, String name) {
        DataPartition<EventData> eventPartition = DataStore.get(guildId, DATA_STORE_NAME);
        long newId = eventPartition.getAndIncrementId();
        Map<Long, EventData> events = eventPartition.getData();
        EventData event = new EventData(name);
        event.setId(newId);
        events.put(newId, event);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
        return event.getId();
    }

    /**
     * Retrieves a specific event by its ID.
     * @param eventId The ID of the event to find.
     * @return The Event object, or null if not found.
     */
    private static EventData getEventById(String guildId, long eventId) {
        DataPartition<EventData> eventPartition = DataStore.get(guildId, DATA_STORE_NAME);
        return eventPartition.getData().get(eventId);
    }

    /**
     * Retrieves a list of all created events, sorted by their ID.
     * @return A sorted list of all events.
     */
    public static List<EventData> getAllEvents(String guildId) {
        DataPartition<EventData> eventPartition = DataStore.get(guildId, DATA_STORE_NAME);
        return new ArrayList<>(eventPartition.getData().values())
                .stream()
                .sorted(Comparator.comparing(EventData::getId))
                .collect(Collectors.toList());
    }

    public static List<EventData> getAllEventsGaugingInterest(String guildId) {
        DataPartition<EventData> eventPartition = DataStore.get(guildId, DATA_STORE_NAME);
        return new ArrayList<>(eventPartition.getData().values())
                .stream()
                .filter(EventData::isGaugeInterest)
                .sorted(Comparator.comparing(EventData::getId))
                .collect(Collectors.toList());
    }

    public static void setEventName(String guildId, long index, String newName) {
        EventData event = getEventById(guildId, index);
        event.setName(newName);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static String getEventName(String guildId, long index) {
        EventData event = getEventById(guildId, index);
        return event == null ? "Unknown Event" : event.getName();
    }

    public static void setStartDate(String guildId, long index, long newStartDate) {
        EventData event = getEventById(guildId, index);
        event.setStartDate(newStartDate);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void setEndDate(String guildId, long index, long newEndDate) {
        EventData event = getEventById(guildId, index);
        event.setEndDate(newEndDate);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void setRoleId(String guildId, long index, long newRoleId) {
        EventData event = getEventById(guildId, index);
        event.setRoleId(newRoleId);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static Role getRole(String guildId, long index) {
        EventData event = getEventById(guildId, index);
        return ArisannaBot.getAriGuild().getRoleById(event.getRoleId());
    }

    public static void setChannelId(String guildId, long index, long newChannelId) {
        EventData event = getEventById(guildId, index);
        event.setChannelId(newChannelId);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void setAddress(String guildId, long index, String newAddress) {
        EventData event = getEventById(guildId, index);
        event.setAddress(newAddress);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void setOmnidexLink(String guildId, long index, String newOmnidexLink) {
        EventData event = getEventById(guildId, index);
        event.setOmnidexLink(newOmnidexLink);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void setTicketLink(String guildId, long index, String newTicketLink) {
        EventData event = getEventById(guildId, index);
        event.setTicketLink(newTicketLink);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static boolean isGaugeInterest(String guildId, long index) {
        EventData event = getEventById(guildId, index);
        return event.isGaugeInterest();
    }

    public static void setGaugeInterest(String guildId, long index, boolean newGaugeInterest) {
        EventData event = getEventById(guildId, index);
        event.setGaugeInterest(newGaugeInterest);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static boolean isMemberInterested(String guildId, long eventIndex, Member member) {
        EventData event = getEventById(guildId, eventIndex);
        return event.getInterestedMembers().contains(member.getIdLong());
    }

    public static List<Long> getMembersInterested(String guildId, long eventIndex) {
        EventData event = getEventById(guildId, eventIndex);
        return event.getInterestedMembers();
    }

    public static void addInterestedMember(String guildId, long eventIndex, Member member) {
        EventData event = getEventById(guildId, eventIndex);
        event.addInterestedMember(member.getIdLong());
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static void removeInterestedMember(String guildId, long eventIndex, Member member) {
        EventData event = getEventById(guildId, eventIndex);
        event.removeInterestedMember(member.getIdLong());
        DataStore.markDirty(guildId, DATA_STORE_NAME);
    }

    public static boolean deleteEvent(String guildId, long index) {
        EventData event = getEventById(guildId, index);
        if (event == null) return false;
        DataPartition<EventData> eventPartition = DataStore.get(guildId, DATA_STORE_NAME);
        Map<Long, EventData> events = eventPartition.getData();
        events.remove(index);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
        return true;
    }

    public static MessageCreateData generateListMessage(String guildId, String authorId, int page) {
        List<EventData> events = EventManager.getAllEvents(guildId);
        if (events.isEmpty()) {
            return new MessageCreateBuilder().setContent("No events found matching criteria.").build();
        }
        return new MessageCreateBuilder()
                .addComponents(buildListContainer(events, page, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildListContainer(List<EventData> events, int page, String authorId) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) events.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of All Events"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        //Add Text Display for current filter here

        for (int i = 0; i < itemsPerPage && (startIndex + i) < events.size(); i++) {
            EventData event = events.get(startIndex + i);
            components.add(TextDisplay.of("### " + event.getName()));
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, "event:list-zoom:" + authorId + ":" + event.getId(), "Details")));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(ArisannaBot.buildListActionRow("event", events, authorId, page));

        return Container.of(components);
    }

    public static MessageCreateData generateGaugeInterestList(String guildId, int page) {
        List<EventData> events = EventManager.getAllEventsGaugingInterest(guildId);
        if (events.isEmpty()) {
            return new MessageCreateBuilder().setContent("We are not currently gauging interest in any events.").build();
        }
        return new MessageCreateBuilder()
                .addComponents(buildGaugeInterestListContainer(events, page))
                .useComponentsV2()
                .build();
    }

    public static Container buildGaugeInterestListContainer(List<EventData> events, int page) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) events.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of Upcoming Events"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        //Add Text Display for current filter here

        for (EventData event : events) {
            //components.add(TextDisplay.of("### " + event.getName()));
            components.add(ActionRow.of(
                    Button.secondary("event:gaugeInterest::" + event.getId(), event.getName() + " - " + event.getInterestedMembers().size() + " Interested")
            ));
            //components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        /* Page related stuff, I don't think we need at this moment.
        for (int i = 0; i < itemsPerPage && (startIndex + i) < events.size(); i++) {
            EventData event = events.get(startIndex + i);
            components.add(TextDisplay.of("### " + event.getName()));
            components.add(ActionRow.of(
                    Button.secondary("event:gaugeInterest:" + authorId + ":" + event.getId(), "I am Interested")
            ));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(buildListActionRow(events, authorId, page));
        */

        return Container.of(components);
    }

    public static MessageCreateData generateDetailMessage(String guildId, String authorId, int index) {
        return new MessageCreateBuilder()
                .addComponents(buildDetailContainer(guildId, index, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildDetailContainer(String guildId, int index, String authorId) {
        EventData event = getEventById(guildId, index);

        if (event == null) {
            log.error("Could not find Event # {} to show Details.", index);
            return buildListContainer(EventManager.getAllEvents(guildId), 0, authorId);
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Event Details: " + event.getName()));

        if (event.getStartDate() > 0 && event.getEndDate() > 0)
            components.add(TextDisplay.of("### Date\n<t:" + event.getStartDate() / 1000 + ":D> to <t:" + event.getEndDate() / 1000 + ":D>"));

        if (event.getAddress() != null && !event.getAddress().isEmpty())
            components.add(TextDisplay.of("### Address\n" + event.getAddress()));

        if (event.getOmnidexLink() != null) if (!event.getOmnidexLink().isEmpty()) if (event.getOmnidexLink().startsWith("https://omni.gatcg.com/events/"))
            components.add(ActionRow.of(Button.link(event.getOmnidexLink(), "Omnidex Link")));

        if (event.getTicketLink() != null) if (!event.getTicketLink().isEmpty())
            components.add(ActionRow.of(Button.link(event.getTicketLink(), "Ticket Link")));

        if (event.getChannelId() > 0)
            components.add(TextDisplay.of("### Channel\n<#" + event.getChannelId() + ">"));

        if (event.getRoleId() > 0)
            components.add(TextDisplay.of("### Role\n<@&" + event.getRoleId() + ">"));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("-# Event ID: " + event.getId()));
        components.add(ActionRow.of(
                Button.primary("event:edit:" + authorId + ":" + index, "Edit"),
                Button.danger("event:detail-back:" + authorId, "List")
        ));

        return Container.of(components);
    }

    public static Container buildEditContainer(String guildId, int index, String authorId) {
        EventData event = getEventById(guildId, index);

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Editing Event # " + event.getId()));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("Click on the different buttons/drop downs to edit values for this event."));
        components.add(ActionRow.of(
                Button.secondary("event:edit-name:" + authorId + ":" + event.getId(), "Name"),
                Button.secondary("event:edit-dates:" + authorId + ":" + event.getId(), "Dates"),
                Button.secondary("event:edit-address:" + authorId + ":" + event.getId(), "Address"),
                Button.secondary("event:edit-omnidex:" + authorId + ":" + event.getId(), "Omnidex"),
                Button.secondary("event:edit-ticket:" + authorId + ":" + event.getId(), "Ticket")
        ));

        components.add(TextDisplay.of("Related Channel"));
        EntitySelectMenu.Builder channelMenu = EntitySelectMenu.create("event:edit-channel:" + authorId + ":" + event.getId(), EntitySelectMenu.SelectTarget.CHANNEL);
        if (event.getChannelId() > 0)
            channelMenu.setDefaultValues(EntitySelectMenu.DefaultValue.channel(event.getChannelId()));
        components.add(ActionRow.of(channelMenu.build()));

        components.add(TextDisplay.of("Related Role"));
        EntitySelectMenu.Builder roleMenu = EntitySelectMenu.create("event:edit-role:" + authorId + ":" + event.getId(), EntitySelectMenu.SelectTarget.ROLE).setMinValues(0);
        if (event.getRoleId() > 0)
            roleMenu.setDefaultValues(EntitySelectMenu.DefaultValue.role(event.getRoleId()));
        components.add(ActionRow.of(roleMenu.build()));

        components.add(TextDisplay.of("Event Actions"));
        components.add(ActionRow.of(
                Button.primary("event:edit-gaugeInterest:" + authorId + ":" + event.getId(), "Gauge Interest"),
                Button.secondary("event:edit-interestedMembers:" + authorId + ":" + event.getId(), event.getInterestedMembers().size() + " Interested").withDisabled(!event.isGaugeInterest()),
                Button.danger("event:edit-delete:" + authorId + ":" + event.getId(), "Delete Event")
        ));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(
                Button.primary("event:edit-view:" + authorId + ":" + event.getId(), "View Event"),
                Button.secondary("event:edit-view-list:" + authorId + ":" + event.getId(), "View List")
        ));

        return Container.of(components);
    }

    public static Modal generateEditNameModal(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        return Modal.create("event:edit-name:" + eventIndex, "Edit Name of Event # " + event.getId())
                .addComponents(
                        Label.of("Name", TextInput.create("name", TextInputStyle.SHORT).setPlaceholder(event.getName()).build())
                ).build();
    }

    public static Modal generateEditDatesModal(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        Label startDate = Label.of("Starting Date", TextInput.create("start-date", TextInputStyle.SHORT)
                .setPlaceholder(event.getStartDate() > 0 ? longToDateString(event.getStartDate()) : "01/01/2020")
                .setRequired(false)
                .build());

        Label endDate = Label.of("Ending Date", TextInput.create("end-date", TextInputStyle.SHORT)
                .setPlaceholder(event.getEndDate() > 0 ? longToDateString(event.getEndDate()) : "12/31/2075")
                .setRequired(false)
                .build());

        return Modal.create("event:edit-dates:" + eventIndex, "Edit Dates for Event # " + event.getId())
                .addComponents(startDate, endDate)
                .build();
    }

    private static String longToDateString(long epochMilli) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Instant instant = Instant.ofEpochMilli(epochMilli);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }

    public static Modal generateEditAddressModal(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        return Modal.create("event:edit-address:" + eventIndex, "Edit Address of Event # " + event.getId())
                .addComponents(
                        Label.of("Address", TextInput.create("address", TextInputStyle.SHORT)
                                .setPlaceholder(event.getAddress().isBlank() ? "123 Main Street" : event.getAddress())
                                .build())
                ).build();
    }

    public static Modal generateEditOmnidexModal(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        return Modal.create("event:edit-omnidex:" + eventIndex, "Edit Omnidex Link on Event # " + event.getId())
                .addComponents(
                        Label.of("Omnidex", TextInput.create("omnidex", TextInputStyle.SHORT)
                                .setPlaceholder(event.getOmnidexLink().isBlank() ? "https://omni.gatcg.com/events/..." : event.getOmnidexLink())
                                .build())
                ).build();
    }

    public static Modal generateEditTicketLinkModal(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        return Modal.create("event:edit-ticket:" + eventIndex, "Edit Ticket Link on Event # " + event.getId())
                .addComponents(
                        Label.of("Ticket", TextInput.create("ticket", TextInputStyle.SHORT)
                                .setPlaceholder(event.getTicketLink().isBlank() ? "https://www.website.com/..." : event.getTicketLink())
                                .build())
                ).build();
    }

    public static Modal generateDeleteEventModel(String guildId, int eventIndex) {
        EventData event = getEventById(guildId, eventIndex);

        if (event == null) {
            log.error("Could not find Event # {} to Delete.", eventIndex);
            return null;
        }

        return Modal.create("event:edit-delete:" + eventIndex, "Delete Event # " + event.getId())
                .addComponents(
                        Label.of("Enter Event Name to Confirm Deletion", TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder(event.getName().isBlank() ? "Unnamed Event" : event.getName())
                                .build())
                ).build();
    }

}
