package dev.supersand24.grandarchive;

import java.util.List;

public class Card {

    private String uuid;
    private String slug;
    private String name;
    private String effect;
    private String flavor;

    private String element;
    private List<String> elements;

    private List<String> classes;
    private List<String> types;
    private List<String> subtypes;

    public static class CardCost {
        private String type;
        private String value;

        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private CardCost cost;

    private Integer level;
    private Integer life;
    private Integer power;
    private Integer durability;

    private List<CardEdition> editions;


    // --- Getters ---
    public String getUuid() { return uuid; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getEffect() { return effect; }
    public String getFlavor() { return flavor; }
    public String getElement() { return element; }
    public List<String> getElements() { return elements; }
    public List<String> getTypes() { return types; }
    public List<String> getClasses() { return classes; }
    public List<String> getSubtypes() { return subtypes; }
    public CardCost getCost() { return cost; }
    public Integer getLevel() { return level; }
    public Integer getLife() { return life; }
    public Integer getPower() { return power; }
    public Integer getDurability() { return durability; }
    public List<CardEdition> getEditions() { return editions; }


    public CardEdition getDefaultEdition() {
        if (editions == null || editions.isEmpty()) return null;

        for (CardEdition edition : editions) {
            if (edition.isDefault()) {
                return edition;
            }
        }
        // Fallback to the first edition if no default is explicitly flagged
        return editions.getFirst();
    }

}
