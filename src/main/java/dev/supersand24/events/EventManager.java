package dev.supersand24.events;

import dev.supersand24.ArisannaBot;
import dev.supersand24.DataPartition;
import dev.supersand24.DataStore;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.modals.Modal;
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
    public static long createEvent(String name) {
        DataPartition<Event> eventPartition = DataStore.get(DATA_STORE_NAME);
        long newId = eventPartition.getAndIncrementId();
        Map<Long, Event> events = eventPartition.getData();
        Event event = new Event(name);
        event.setId(newId);
        events.put(newId, event);
        DataStore.markDirty(DATA_STORE_NAME);
        return event.getId();
    }

    /**
     * Retrieves a specific event by its ID.
     * @param eventId The ID of the event to find.
     * @return The Event object, or null if not found.
     */
    private static Event getEventById(long eventId) {
        DataPartition<Event> eventPartition = DataStore.get(DATA_STORE_NAME);
        return eventPartition.getData().get(eventId);
    }

    /**
     * Retrieves a list of all created events, sorted by their ID.
     * @return A sorted list of all events.
     */
    public static List<Event> getAllEvents() {
        DataPartition<Event> eventPartition = DataStore.get(DATA_STORE_NAME);
        return new ArrayList<>(eventPartition.getData().values())
                .stream()
                .sorted(Comparator.comparing(Event::getId))
                .collect(Collectors.toList());
    }

    public static void setEventName(long index, String newName) {
        Event event = getEventById(index);
        event.setName(newName);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static String getEventName(long index) {
        Event event = getEventById(index);
        return event == null ? "Unknown Event" : event.getName();
    }

    public static void setStartDate(long index, long newStartDate) {
        Event event = getEventById(index);
        event.setStartDate(newStartDate);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static void setEndDate(long index, long newEndDate) {
        Event event = getEventById(index);
        event.setEndDate(newEndDate);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static void setRoleId(long index, long newRoleId) {
        Event event = getEventById(index);
        event.setRoleId(newRoleId);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static Role getRole(long index) {
        Event event = getEventById(index);
        return ArisannaBot.getAriGuild().getRoleById(event.getRoleId());
    }

    public static void setChannelId(long index, long newChannelId) {
        Event event = getEventById(index);
        event.setChannelId(newChannelId);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static void setAddress(long index, String newAddress) {
        Event event = getEventById(index);
        event.setAddress(newAddress);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static void setOmnidexLink(long index, String newOmnidexLink) {
        Event event = getEventById(index);
        event.setOmnidexLink(newOmnidexLink);
        DataStore.markDirty(DATA_STORE_NAME);
    }

    public static boolean deleteEvent(long index) {
        Event event = getEventById(index);
        if (event == null) return false;
        DataPartition<Event> eventPartition = DataStore.get(DATA_STORE_NAME);
        Map<Long, Event> events = eventPartition.getData();
        events.remove(index);
        DataStore.markDirty(DATA_STORE_NAME);
        return true;
    }

    public static MessageCreateData generateListMessage(String authorId, int page) {
        List<Event> events = EventManager.getAllEvents();
        if (events.isEmpty()) {
            return new MessageCreateBuilder().setContent("No events found matching criteria.").build();
        }
        return new MessageCreateBuilder()
                .addComponents(buildListContainer(events, page, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildListContainer(List<Event> events, int page, String authorId) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) events.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of All Events"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        //Add Text Display for current filter here

        for (int i = 0; i < itemsPerPage && (startIndex + i) < events.size(); i++) {
            Event event = events.get(startIndex + i);
            components.add(TextDisplay.of("### " + event.getName()));
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, "event-list-zoom:" + authorId + ":" + event.getId(), "Details")));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(buildListActionRow(events, authorId, page));

        return Container.of(components);
    }

    private static ActionRow buildListActionRow(List<Event> events, String authorId, int page) {
        int totalPages = (int) Math.ceil((double) events.size() / 5.0);

        Button prev = Button.secondary("event-list-prev:" + authorId + ":" + page, "◀️ Previous").withDisabled(page == 0);
        Button next = Button.secondary("event-list-next:" + authorId + ":" + page, "Next ▶️").withDisabled(page >= totalPages - 1);

        return ActionRow.of(prev, next);
    }

    public static MessageCreateData generateDetailMessage(String authorId, int index) {
        return new MessageCreateBuilder()
                .addComponents(buildDetailContainer(index, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildDetailContainer(int index, String authorId) {
        Event event = getEventById(index);

        if (event == null) {
            log.error("Could not find Event # {} to show Details.", index);
            return buildListContainer(EventManager.getAllEvents(), 0, authorId);
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Event Details: " + event.getName()));

        if (event.getStartDate() > 0 && event.getEndDate() > 0)
            components.add(TextDisplay.of("### Date\n<t:" + event.getStartDate() / 1000 + ":D> to <t:" + event.getEndDate() / 1000 + ":D>"));

        if (event.getAddress() != null && !event.getAddress().isEmpty())
            components.add(TextDisplay.of("### Address\n" + event.getAddress()));

        if (event.getOmnidexLink() != null) if (!event.getOmnidexLink().isEmpty()) if (event.getOmnidexLink().startsWith("https://omni.gatcg.com/events/"))
            components.add(ActionRow.of(Button.link(event.getOmnidexLink(), "Omnidex")));

        if (event.getChannelId() > 0)
            components.add(TextDisplay.of("### Channel\n<#" + event.getChannelId() + ">"));

        if (event.getRoleId() > 0)
            components.add(TextDisplay.of("### Role\n<@&" + event.getRoleId() + ">"));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("-# Event ID: " + event.getId()));
        components.add(ActionRow.of(
                Button.primary("event-edit:" + authorId + ":" + index, "Edit"),
                Button.danger("event-detail-back:" + authorId, "List")
        ));

        return Container.of(components);
    }

    public static Container generateEditContainer(int index, String authorId) {
        Event event = getEventById(index);

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Editing Event # " + event.getId()));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("Click on the different buttons/drop downs to edit values for this event."));
        components.add(ActionRow.of(
                Button.secondary("event-edit-name:" + authorId + ":" + event.getId(), "Name"),
                Button.secondary("event-edit-dates:" + authorId + ":" + event.getId(), "Dates"),
                Button.secondary("event-edit-address:" + authorId + ":" + event.getId(), "Address"),
                Button.secondary("event-edit-omnidex:" + authorId + ":" + event.getId(), "Omnidex")
        ));

        components.add(TextDisplay.of("Related Channel"));
        EntitySelectMenu.Builder channelMenu = EntitySelectMenu.create("event-edit-channel:" + authorId + ":" + event.getId(), EntitySelectMenu.SelectTarget.CHANNEL);
        if (event.getChannelId() > 0)
            channelMenu.setDefaultValues(EntitySelectMenu.DefaultValue.channel(event.getChannelId()));
        components.add(ActionRow.of(channelMenu.build()));

        components.add(TextDisplay.of("Related Role"));
        EntitySelectMenu.Builder roleMenu = EntitySelectMenu.create("event-edit-role:" + authorId + ":" + event.getId(), EntitySelectMenu.SelectTarget.ROLE);
        if (event.getRoleId() > 0)
            roleMenu.setDefaultValues(EntitySelectMenu.DefaultValue.role(event.getRoleId()));
        components.add(ActionRow.of(roleMenu.build()));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(
                Button.primary("event-edit-view:" + authorId + ":" + event.getId(), "View Event"),
                Button.secondary("event-edit-view-list:" + authorId + ":" + event.getId(), "View List"),
                Button.danger("event-edit-delete:" + authorId + ":" + event.getId(), "Delete Event")
        ));

        return Container.of(components);
    }

    public static Modal generateEditNameModal(int eventIndex) {
        Event event = getEventById(eventIndex);

        return Modal.create("event-edit-name:" + eventIndex, "Edit Name of Event # " + event.getId())
                .addComponents(ActionRow.of(TextInput.create("name", "Name", TextInputStyle.SHORT)
                        .setPlaceholder(event.getName())
                        .build()))
                .build();
    }

    public static Modal generateEditDatesModal(int eventIndex) {
        Event event = getEventById(eventIndex);

        TextInput startDate = TextInput.create("start-date", "Starting Date", TextInputStyle.SHORT)
                .setPlaceholder(event.getStartDate() > 0 ? longToDateString(event.getStartDate()) : "01/01/2020")
                .setRequired(false)
                .build();

        TextInput endDate = TextInput.create("end-date", "Ending Date", TextInputStyle.SHORT)
                .setPlaceholder(event.getEndDate() > 0 ? longToDateString(event.getEndDate()) : "12/31/2025")
                .setRequired(false)
                .build();

        return Modal.create("event-edit-dates:" + eventIndex, "Edit Dates for Event # " + event.getId())
                .addComponents(ActionRow.of(startDate), ActionRow.of(endDate))
                .build();
    }

    private static String longToDateString(long epochMilli) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        Instant instant = Instant.ofEpochMilli(epochMilli);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }

    public static Modal generateEditAddressModal(int eventIndex) {
        Event event = getEventById(eventIndex);

        return Modal.create("event-edit-address:" + eventIndex, "Edit Address of Event # " + event.getId())
                .addComponents(ActionRow.of(TextInput.create("address", "Address", TextInputStyle.SHORT)
                        .setPlaceholder(event.getAddress() == null ? "123 Main Street" : event.getAddress())
                        .build()))
                .build();
    }

    public static Modal generateEditOmnidexModal(int eventIndex) {
        Event event = getEventById(eventIndex);

        return Modal.create("event-edit-omnidex:" + eventIndex, "Edit Omnidex Link on Event # " + event.getId())
                .addComponents(ActionRow.of(TextInput.create("omnidex", "Omnidex", TextInputStyle.SHORT)
                        .setPlaceholder(event.getOmnidexLink() == null ? "https://omni.gatcg.com/events/..." : event.getOmnidexLink())
                        .build()))
                .build();
    }

    public static Modal generateDeleteEventModel(int eventIndex) {
        Event event = getEventById(eventIndex);

        if (event == null) {
            log.error("Could not find Event # {} to Delete.", eventIndex);
            return null;
        }

        return Modal.create("event-edit-delete:" + eventIndex, "Delete Event # " + event.getId())
                .addComponents(ActionRow.of(TextInput.create("name", "Enter Event Name to Confirm Deletion.", TextInputStyle.SHORT)
                        .setPlaceholder(event.getName())
                        .build()))
                .build();
    }

}
