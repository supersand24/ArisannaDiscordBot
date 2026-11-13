package dev.supersand24.grandarchive;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Card {
    private String uuid;
    private String name;
    private String element;
    private List<String> elements;
    private String effect;

    private List<String> types;
    private List<String> classes;
    private List<String> subtypes;

    public static class CardCost {
        private String type;
        private String value;
        public String getType() { return type; }
        public String getValue() { return value; }
    }
    private CardCost cost;

    private Integer life;
    private Integer power;
    private Integer durability;

    private List<CardEdition> editions;

    // Catches the nested flip-side printing
    @SerializedName("edition")
    private CardEdition singularEdition;

    public String getUuid() { return uuid; }
    public String getName() { return name; }
    public String getElement() { return element; }
    public List<String> getElements() { return elements; }
    public String getEffect() { return effect; }
    public List<String> getTypes() { return types; }
    public List<String> getClasses() { return classes; }
    public List<String> getSubtypes() { return subtypes; }
    public CardCost getCost() { return cost; }
    public Integer getLife() { return life; }
    public Integer getPower() { return power; }
    public Integer getDurability() { return durability; }

    public List<CardEdition> getEditions() { return editions; }
    public CardEdition getSingularEdition() { return singularEdition; }

    public void setEditions(List<CardEdition> editions) {
        this.editions = editions;
    }

    public void addEdition(CardEdition edition) {
        if (this.editions == null) this.editions = new ArrayList<>();
        this.editions.add(edition);
    }

    public CardEdition getDefaultEdition() {
        if (editions == null || editions.isEmpty()) return singularEdition;
        return editions.stream().filter(CardEdition::isDefaultEdition).findFirst().orElse(editions.getFirst());
    }
}