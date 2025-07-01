package dev.supersand24;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class CounterData {
    
    public transient String name;
    public int value;
    public List<String> allowedEditors = new ArrayList<>();
    public String description;
    public Integer maxValue;
    public Integer minValue;
    public Integer incrementAmount = 1;
    public Integer decrementAmount = 1;

    public CounterData() {
        
    }

    public CounterData(String name, String description, int initialValue, int minValue, int maxValue, String creator) {
        this.name = name;
        this.description = description;
        this.value = initialValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.allowedEditors.add(creator);
    }

    public void set(int amount) {
        value = amount;
        if (maxValue != null) value = Math.min(value, maxValue);
        if (minValue != null) value = Math.max(value, minValue);
    }

    public void increment() {
        value += incrementAmount;
        if (maxValue != null) value = Math.min(value, maxValue);
    }

    public void decrement() {
        value -= decrementAmount;
        if (minValue != null) value = Math.max(value, minValue);
    }

    public MessageEmbed toEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(name);
        embed.setDescription(description);
        embed.addField("Value", Integer.toString(value), true);
        return embed.build();
    }

}
