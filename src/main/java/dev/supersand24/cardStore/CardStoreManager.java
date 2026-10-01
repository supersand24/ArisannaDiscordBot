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

    public static String getIdentifier() {
        return "card-store";
    }

    public static long createCardStore(String guildId, String storeName) {
        DataPartition<CardStoreData> storePartition = DataStore.get(guildId, DATA_STORE_NAME);
        long newId = storePartition.getAndIncrementId();
        Map<Long, CardStoreData> stores = storePartition.getData();
        CardStoreData cardStore = new CardStoreData();
        cardStore.setId(newId);
        cardStore.setName(storeName);
        stores.put(newId, cardStore);
        DataStore.markDirty(guildId, DATA_STORE_NAME);
        return cardStore.getId();
    }

    private static CardStoreData getCardStoreById(String guildId, long eventId) {
        DataPartition<CardStoreData> storePartition = DataStore.get(guildId, DATA_STORE_NAME);
        return storePartition.getData().get(eventId);
    }

    public static List<CardStoreData> getAllCardStores(String guildId) {
        DataPartition<CardStoreData> storePartition = DataStore.get(guildId, DATA_STORE_NAME);
        return new ArrayList<>(storePartition.getData().values())
                .stream()
                .sorted(Comparator.comparing(CardStoreData::getId))
                .collect(Collectors.toList());
    }

    public static MessageCreateData generateListMessage(String guildId, String authorId, int page) {
        List<CardStoreData> events = getAllCardStores(guildId);
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
            components.add(ActionRow.of(Button.of(ButtonStyle.SECONDARY, getIdentifier() + ":list-zoom:" + authorId + ":" + cardStore.getId(), "Details")));
            components.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        components.add(TextDisplay.of("-# Page " + (page + 1) + " of " + totalPages));
        components.add(ArisannaBot.buildListActionRow(getIdentifier(), stores, authorId, page));

        return Container.of(components);
    }

    public static MessageCreateData generateDetailMessage(String guildId, String authorId, int index) {
        return new MessageCreateBuilder()
                .addComponents(buildDetailContainer(guildId, index, authorId))
                .useComponentsV2()
                .build();
    }

    public static Container buildDetailContainer(String guildId, int index, String authorId) {
        CardStoreData store = getCardStoreById(guildId, index);

        if (store == null) {
            log.error("Could not find Card Store #{} to show Details.", index);
            return buildListContainer(getAllCardStores(guildId), 0, authorId);
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
                Button.primary(getIdentifier() + ":edit:" + authorId + ":" + index, "Edit"),
                Button.danger(getIdentifier() + ":detail-back:" + authorId, "List")
        ));

        return Container.of(components);
    }

    public static Container generateEditContainer(String guildId, int index, String authorId) {
        CardStoreData store = getCardStoreById(guildId, index);

        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("## Editing Card Store # " + store.getId()));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("Click on the different buttons/drop downs to edit values for this store."));
        components.add(ActionRow.of(
                Button.secondary(getIdentifier() + ":edit-name:" + authorId + ":" + store.getId(), "Name"),
                Button.secondary(getIdentifier() + ":edit-address:" + authorId + ":" + store.getId(), "Address"),
                Button.secondary(getIdentifier() + ":edit-website:" + authorId + ":" + store.getId(), "Website")
        ));

        components.add(TextDisplay.of("Card Store Actions"));
        components.add(ActionRow.of(
                Button.danger(getIdentifier() + ":edit-delete:" + authorId + ":" + store.getId(), "Delete Card Store")
        ));

        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(ActionRow.of(
                Button.primary(getIdentifier() + ":edit-view:" + authorId + ":" + store.getId(), "View Store"),
                Button.secondary(getIdentifier() + ":edit-view-list:" + authorId + ":" + store.getId(), "View List")
        ));

        return Container.of(components);
    }

    public static Modal generateEditNameModal(String guildId, int eventIndex) {
        CardStoreData store = getCardStoreById(guildId, eventIndex);

        return Modal.create(getIdentifier() + ":edit-name:" + eventIndex, "Edit Name of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Name", TextInput.create("name", TextInputStyle.SHORT).setPlaceholder(store.getName()).build())
                ).build();
    }

    public static Modal generateEditAddressModal(String guildId, int eventIndex) {
        CardStoreData store = getCardStoreById(guildId, eventIndex);

        return Modal.create(getIdentifier() + ":edit-address:" + eventIndex, "Edit Address of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Address", TextInput.create("address", TextInputStyle.SHORT)
                                .setPlaceholder(store.getAddress().isBlank() ? "123 Main Street" : store.getAddress())
                                .build())
                ).build();
    }

    public static Modal generateEditWebsiteModal(String guildId, int eventIndex) {
        CardStoreData store = getCardStoreById(guildId, eventIndex);

        return Modal.create(getIdentifier() + ":edit-website:" + eventIndex, "Edit Website Link of Card Store # " + store.getId())
                .addComponents(
                        Label.of("Website", TextInput.create("website", TextInputStyle.SHORT)
                                .setPlaceholder(store.getWebsite().isBlank() ? "https://www.website.com/..." : store.getWebsite())
                                .build())
                ).build();
    }

    public static Modal generateDeleteCardStoreModel(String guildId, int eventIndex) {
        CardStoreData store = getCardStoreById(guildId, eventIndex);

        if (store == null) {
            log.error("Could not find Card Store # {} to Delete.", eventIndex);
            return null;
        }

        return Modal.create(getIdentifier() + ":edit-delete:" + eventIndex, "Delete Card Store # " + store.getId())
                .addComponents(
                        Label.of("Enter Card Store Name to Confirm Deletion", TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder(store.getName())
                                .build())
                ).build();
    }

}
