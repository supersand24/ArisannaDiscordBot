package dev.supersand24.grandarchive;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardCache {

    private static final Logger log = LoggerFactory.getLogger(CardCache.class);
    private static final Gson gson = new Gson();

    private static final Map<String, Card> cardMap = new HashMap<>();
    private static final Map<String, Card> cardUuidMap = new HashMap<>();
    public static class OptionItem {
        private String text;
        private String value;

        public String getText() { return text; }
        public String getValue() { return value; }
    }
    private static class OptionsData {
        @SerializedName("class") List<OptionItem> classes;
        List<OptionItem> elements;
        List<OptionItem> subtypes;
        List<OptionItem> types;
    }
    private static OptionsData data;

    public static void loadCache() {
        Path optionsFilePath = Paths.get("data/options.json");

        if (!Files.exists(optionsFilePath)) {
            log.warn("data/options.json not found! Run the /sync command.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(optionsFilePath, StandardCharsets.UTF_8)) {
            data = gson.fromJson(reader, OptionsData.class);
            log.info("Successfully loaded Grand Archive options into memory.");
        } catch (Exception e) {
            log.error("Failed to load options.json into memory.", e);
        }

        Path cardsFilePath = Paths.get("data/cards.json");

        if (!Files.exists(cardsFilePath)){
            log.warn("data/cards.json not found! Run the /sync command to download data.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(cardsFilePath, StandardCharsets.UTF_8)){
            List<Card> cards = gson.fromJson(reader, new TypeToken<List<Card>>(){}.getType());

            cardMap.clear();
            cardUuidMap.clear();

            for (Card card : cards) {
                if (card.getName() != null) { cardMap.put(card.getName().toLowerCase(), card); }
                if (card.getUuid() != null) { cardUuidMap.put(card.getUuid(), card); }

                if (card.getEditions() != null) {
                    for (CardEdition ed : card.getEditions()) {

                        if (ed.getOtherOrientations() != null) {
                            for (Card flipSideData : ed.getOtherOrientations()) {
                                String flipUuid = flipSideData.getUuid();
                                CardEdition backEdition = flipSideData.getSingularEdition();

                                if (backEdition != null) {
                                    backEdition.setParentCardUuid(card.getUuid());
                                    backEdition.setParentEditionUuid(ed.getUuid());
                                }

                                if (cardUuidMap.containsKey(flipUuid)) {
                                    if (backEdition != null) {
                                        cardUuidMap.get(flipUuid).addEdition(backEdition);
                                    }
                                } else {
                                    if (backEdition != null){
                                        flipSideData.setEditions(new ArrayList<>(List.of(backEdition)));
                                    }
                                    cardUuidMap.put(flipUuid, flipSideData);

                                    if (flipSideData.getName() != null) {
                                        cardMap.put(flipSideData.getName().toLowerCase(), flipSideData);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            log.info("Successfully loaded {} Grand Archive cards into memory.", cardMap.size());
        } catch (Exception e) {
            log.error("Failed to load cards.json into memory.", e);
        }
    }

    public static Card getCard(String name) {
        if (name == null) return null;
        return cardMap.get(name.toLowerCase());
    }

    public static Card getCardByUuid(String uuid) {
        if (uuid == null) return null;
        return cardUuidMap.get(uuid);
    }

    public static Map<String, Card> getAllCards() {
        return cardMap;
    }

    public static List<OptionItem> getClasses() { return data.classes != null ? data.classes : List.of(); }
    public static List<OptionItem> getElements() { return data.elements != null ? data.elements : List.of(); }
    public static List<OptionItem> getTypes() { return data.types != null ? data.types : List.of(); }
    public static List<OptionItem> getSubtypes() { return data.subtypes != null ? data.subtypes : List.of(); }
}
