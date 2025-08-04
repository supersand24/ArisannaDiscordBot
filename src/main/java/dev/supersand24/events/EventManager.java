package dev.supersand24.events;

import dev.supersand24.DataPartition;
import dev.supersand24.DataStore;
import dev.supersand24.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
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

    public static MessageCreateData sendListView(String authorId, int page) {
        List<Event> events = EventManager.getAllEvents();
        if (events.isEmpty()) {
            return new MessageCreateBuilder().setContent("No events found matching criteria.").build();
        }
        return new MessageCreateBuilder()
                .addEmbeds(buildListEmbed(events, page).build())
                .setComponents(buildListActionRow(events, authorId, page))
                .build();
    }

    public static MessageCreateData editListView(String authorId, int page) {
        List<Event> events = EventManager.getAllEvents();
        return new MessageCreateBuilder()
                .addEmbeds(buildListEmbed(events, page).build())
                .setComponents(buildListActionRow(events, authorId, page))
                .build();
    }

    public static MessageCreateData editDetailView(String authorId, int index) {
        List<Event> events = EventManager.getAllEvents();
        return new MessageCreateBuilder()
                .addEmbeds(buildDetailEmbed(events, index).build())
                .setComponents(buildDetailActionRow(events, authorId, index))
                .build();
    }

    private static EmbedBuilder buildListEmbed(List<Event> events, int page) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) events.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("List of All Events");
        embed.setColor(Color.MAGENTA);
        embed.setFooter("Page " + (page + 1) + " of " + totalPages);

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < itemsPerPage && (startIndex + i) < events.size(); i++) {
            Event event = events.get(startIndex + i);
            description.append(String.format("`%d`: **%s**\n", event.getEventId(), event.getName()));
        }
        embed.setDescription(description.toString());
        return embed;
    }

    private static List<ActionRow> buildListActionRow(List<Event> events, String authorId, int page) {
        int totalPages = (int) Math.ceil((double) events.size() / 5.0);
        int eventIndex = page * 5;

        Button prev = Button.secondary("event-list-prev:" + authorId + ":" + page, "◀️ Previous Page").withDisabled(page == 0);
        Button next = Button.secondary("event-list-next:" + authorId + ":" + page, "Next Page ▶️").withDisabled(page >= totalPages - 1);

        Builder menu = StringSelectMenu.create("event-list-zoom:" + authorId)
                .setPlaceholder("View details for a specific event...");

        int startIndex = page * 5;
        for (int i = 0; i < 5 && (startIndex + i) < events.size(); i++) {
            Event event = events.get(startIndex + i);
            menu.addOption(event.getName(), String.valueOf(startIndex + i));
        }

        return List.of(ActionRow.of(prev, next), ActionRow.of(menu.build()));
    }

    private static EmbedBuilder buildDetailEmbed(List<Event> events, int index) {
        Event event = events.get(index);
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Event Details: " + event.getName());
        embed.setColor(Color.ORANGE);
        embed.setFooter("Event " + (index + 1) + " of " + events.size() + " • ID: " + event.getEventId());

        if (event.getStartDate() > 0 && event.getEndDate() > 0) {
            embed.addField("Date", "<t:" + event.getStartDate() / 1000 + ":D> to <t:" + event.getEndDate() / 1000 + ":D>", false);
        }
        if (event.getAddress() != null && !event.getAddress().isEmpty()) {
            embed.addField("Address", event.getAddress(), false);
        }
        if (event.getOmnidexLink() != null && !event.getOmnidexLink().isEmpty()) {
            embed.addField("Link", event.getOmnidexLink(), false);
        }
        if (event.getChannelId() > 0) {
            embed.addField("Channel", "<#" + event.getChannelId() + ">", true);
        }
        if (event.getRoleId() > 0) {
            embed.addField("Role", "<@&" + event.getRoleId() + ">", true);
        }
        return embed;
    }

    private static List<ActionRow> buildDetailActionRow(List<Event> events, String authorId, int index) {
        int page = index / 5;

        Button prev = Button.secondary("event-detail-prev:" + authorId + ":" + index, "◀️ Previous Event").withDisabled(index == 0);
        Button next = Button.secondary("event-detail-next:" + authorId + ":" + index, "Next Event ▶️").withDisabled(index >= events.size() - 1);
        Button back = Button.danger("event-detail-back:" + authorId + ":" + page, "Back to List");

        return List.of(ActionRow.of(prev, next), ActionRow.of(back));
    }

}
