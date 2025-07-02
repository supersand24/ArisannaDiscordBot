package dev.supersand24;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.List;
import java.util.function.Function;

public class Paginator<T> {

    private final List<T> items;

    public Paginator(List<T> items) {
        this.items = items;
    }

    public EmbedBuilder getEmbed(int page, int itemsPerPage, String title, Color color, Function<T, String> itemFormatter) {
        int startIndex = page * itemsPerPage;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setColor(color);
        embed.setFooter("Page " + (page + 1) + " of " + getTotalPages(itemsPerPage));

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < itemsPerPage; i++) {
            int currentIndex  = startIndex + i;
            if (currentIndex >= items.size()) break;
            T item = items.get(currentIndex);
            description.append(itemFormatter.apply(item)).append("\n");
        }

        embed.setDescription(description.toString());
        return embed;
    }

    public int getTotalPages(int itemsPerPage) {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

}
