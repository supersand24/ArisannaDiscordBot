package dev.supersand24.cardStore;

import dev.supersand24.ArisannaBot;
import dev.supersand24.DataPartition;
import dev.supersand24.DataStore;
import dev.supersand24.events.EventData;
import dev.supersand24.events.EventManager;
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

public class CardStoreManager {

    private static final Logger log = LoggerFactory.getLogger(CardStoreManager.class);

    private static final String DATA_STORE_NAME = "events";

    public String getIdentifier() {
        return "cardStore";
    }

    public static long createCardStore(String name) {
        DataPartition<CardStoreData> storePartition = DataStore.get(DATA_STORE_NAME);
        long newId = storePartition.getAndIncrementId();
        Map<Long, CardStoreData> stores = storePartition.getData();
        CardStoreData cardStore = new CardStoreData();
        cardStore.setId(newId);
        stores.put(newId, cardStore);
        DataStore.markDirty(DATA_STORE_NAME);
        return cardStore.getId();
    }

    private static CardStoreData getCardStoreById(long eventId) {
        DataPartition<CardStoreData> storePartition = DataStore.get(DATA_STORE_NAME);
        return storePartition.getData().get(eventId);
    }

    public static List<CardStoreData> getAllCardStores() {
        DataPartition<CardStoreData> storePartition = DataStore.get(DATA_STORE_NAME);
        return new ArrayList<>(storePartition.getData().values())
                .stream()
                .sorted(Comparator.comparing(CardStoreData::getId))
                .collect(Collectors.toList());
    }

    public static MessageCreateData generateListMessage(String authorId, int page) {
        List<CardStoreData> events = getAllCardStores();
        if (events.isEmpty()) {
            return new MessageCreateBuilder().setContent("No stores found matching criteria.").build();
        }
        return new MessageCreateBuilder()
                .addComponents(buildListContainer(events, page, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildListContainer(List<CardStoreData> stores, int page, String authorId) {
        final int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) stores.size() / itemsPerPage);
        int startIndex = page * itemsPerPage;

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## List of All Stores"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        //Add Text Display for current filter here

        for (int i = 0; i < itemsPerPage && (startIndex + i) < stores.size(); i++) {
            CardStoreData cardStore = stores.get(startIndex + i);
            components.add(TextDisplay.of("### " + cardStore.getName()));
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, "cardStore:list-zoom:" + authorId + ":" + cardStore.getId(), "Details")));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(ArisannaBot.buildListActionRow("cardStore", stores, authorId, page));

        return Container.of(components);
    }

    public static MessageCreateData generateDetailMessage(String authorId, int index) {
        return new MessageCreateBuilder()
                .addComponents(buildDetailContainer(index, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildDetailContainer(int index, String authorId) {
        CardStoreData store = getCardStoreById(index);

        if (store == null) {
            log.error("Could not find Card Store #{} to show Details.", index);
            return buildListContainer(getAllCardStores(), 0, authorId);
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Store Details: " + store.getName()));

        if (store.getAddress() != null && !store.getAddress().isEmpty())
            components.add(TextDisplay.of("### Address\n" + store.getAddress()));

        if (store.getWebsite() != null && !store.getWebsite().isEmpty())
            components.add(ActionRow.of(Button.link(store.getWebsite(), "Website Link")));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("-# Card Store ID: " + store.getId()));
        components.add(ActionRow.of(
                Button.primary("cardStore:edit:" + authorId + ":" + index, "Edit"),
                Button.danger("cardStore:detail-back:" + authorId, "List")
        ));

        return Container.of(components);
    }

    public static Container generateEditContainer(int index, String authorId) {
        CardStoreData store = getCardStoreById(index);

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Editing Card Store # " + store.getId()));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("Click on the different buttons/drop downs to edit values for this store."));
        components.add(ActionRow.of(
                Button.secondary("cardStore:edit-name:" + authorId + ":" + store.getId(), "Name"),
                Button.secondary("cardStore:edit-address:" + authorId + ":" + store.getId(), "Address"),
                Button.secondary("cardStore:edit-website:" + authorId + ":" + store.getId(), "Website")
        ));

        components.add(TextDisplay.of("Card Store Actions"));
        components.add(ActionRow.of(
                Button.danger("event:edit-delete:" + authorId + ":" + store.getId(), "Delete Event")
        ));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(
                Button.primary("cardStore:edit-view:" + authorId + ":" + store.getId(), "View Event"),
                Button.secondary("cardStore:edit-view-list:" + authorId + ":" + store.getId(), "View List")
        ));

        return Container.of(components);
    }

    public static Modal generateEditNameModal(int eventIndex) {
        CardStoreData store = getCardStoreById(eventIndex);

        return Modal.create("cardStore:edit-name:" + eventIndex, "Edit Name of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Name", TextInput.create("name", TextInputStyle.SHORT).setPlaceholder(store.getName()).build())
                ).build();
    }

    public static Modal generateEditAddressModal(int eventIndex) {
        CardStoreData store = getCardStoreById(eventIndex);

        return Modal.create("cardStore:edit-address:" + eventIndex, "Edit Address of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Address", TextInput.create("address", TextInputStyle.SHORT)
                                .setPlaceholder(store.getAddress().isBlank() ? "123 Main Street" : store.getAddress())
                                .build())
                ).build();
    }

    public static Modal generateEditWebsiteModal(int eventIndex) {
        CardStoreData store = getCardStoreById(eventIndex);

        return Modal.create("cardStore:edit-website:" + eventIndex, "Edit Website Link of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Website", TextInput.create("website", TextInputStyle.SHORT)
                                .setPlaceholder(store.getWebsite().isBlank() ? "https://www.website.com/..." : store.getWebsite())
                                .build())
                ).build();
    }

    public static Modal generateDeleteCardStoreModel(int eventIndex) {
        CardStoreData store = getCardStoreById(eventIndex);

        if (store == null) {
            log.error("Could not find Card Store # {} to Delete.", eventIndex);
            return null;
        }

        return Modal.create("cardStore:edit-delete:" + eventIndex, "Delete Card Store # " + store.getId())
                .addComponents(
                        Label.of("Enter Card Store Name to Confirm Deletion", TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder(store.getName())
                                .build())
                ).build();
    }

}
