package dev.supersand24.events;

import dev.supersand24.DataPartition;
import dev.supersand24.DataStore;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EventManager {

    /**
     * Creates a new event and saves it to the data store.
     * @param name The name of the new event (e.g., "PAX East 2025").
     * @return The newly created Event ID.
     */
    public static long createEvent(String name) {
        DataPartition<Event> eventPartition = DataStore.get("events");
        long newId = eventPartition.getAndIncrementId();
        Event event = new Event(name);
        event.setId(newId);

        eventPartition.getData().put(newId, event);
        DataStore.markDirty("events");

        return newId;
    }

    /**
     * Retrieves a specific event by its ID.
     * @param eventId The ID of the event to find.
     * @return The Event object, or null if not found.
     */
    public static Event getEventById(long eventId) {
        DataPartition<Event> eventPartition = DataStore.get("events");
        return eventPartition.getData().get(eventId);
    }

    /**
     * Retrieves a list of all created events, sorted by their ID.
     * @return A sorted list of all events.
     */
    public static List<Event> getAllEvents() {
        DataPartition<Event> eventPartition = DataStore.get("events");
        return new ArrayList<>(eventPartition.getData().values())
                .stream()
                .sorted(Comparator.comparing(Event::getEventId))
                .collect(Collectors.toList());
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

    private static Container buildListContainer(List<Event> events, int page, String authorId) {
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
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, "event-list-zoom:" + authorId + ":" + (startIndex + i), "Details")));
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
        List<Event> events = EventManager.getAllEvents();
        return new MessageCreateBuilder()
                .addComponents(buildDetailContainer(events, index, authorId))
                .useComponentsV2()
                .build();
    }

    private static Container buildDetailContainer(List<Event> events, int index, String authorId) {
        Event event = events.get(index);

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Event Details: " + event.getName()));

        if (event.getStartDate() > 0 && event.getEndDate() > 0) {
            components.add(TextDisplay.of("Date\n<t:" + event.getStartDate() / 1000 + ":D> to <t:" + event.getEndDate() / 1000 + ":D>"));
        }

        if (event.getAddress() != null && !event.getAddress().isEmpty()) {
            components.add(TextDisplay.of("Address: " + event.getAddress()));
        }

        if (event.getOmnidexLink() != null && !event.getOmnidexLink().isEmpty()) {
            components.add(ActionRow.of(Button.link(event.getOmnidexLink(), "Omnidex")));
        }

        if (event.getChannelId() > 0) {
            components.add(TextDisplay.of("<#" + event.getChannelId() + ">"));
        }

        if (event.getRoleId() > 0) {
            components.add(TextDisplay.of("<@&" + event.getRoleId() + ">"));
        }

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("-# Event " + (index + 1) + " of " + events.size() + " • ID: " + event.getEventId()));
        components.add(buildDetailActionRow(events, authorId, index));

        return Container.of(components);
    }

    private static ActionRow buildDetailActionRow(List<Event> events, String authorId, int index) {
        int page = index / 5;

        Button prev = Button.secondary("event-detail-prev:" + authorId + ":" + index, "◀️ Previous").withDisabled(index == 0);
        Button next = Button.secondary("event-detail-next:" + authorId + ":" + index, "Next ▶️").withDisabled(index >= events.size() - 1);
        Button back = Button.danger("event-detail-back:" + authorId + ":" + page, "List");

        return ActionRow.of(prev, next, back);
    }

}
