package dev.supersand24;

import java.util.List;

import org.w3c.dom.css.Counter;

public class CounterData {
    
    public String name;
    public int value;
    public List<String> allowedEditors;
    public String description;
    public Integer maxValue;
    public Integer minValue;

    public CounterData() {

    }

    public CounterData(String name, int initialValue, List<String> allowedEditors) {
        this.name = name;
        this.value = initialValue;
        this.allowedEditors = allowedEditors;
    }

    public void adjust(int amount) {
        value += amount;
        if (maxValue != null) value = Math.min(value, maxValue);
        if (minValue != null) value = Math.max(value, minValue);
    }

}
