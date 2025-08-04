package dev.supersand24.expenses;

import dev.supersand24.CurrencyUtils;
import dev.supersand24.Identifiable;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExpenseData implements Identifiable {

    public transient long expenseId;
    private long eventId;
    public String name;
    public double amount;
    public String payerId;
    public List<String> beneficiaryIds = new ArrayList<>();
    private long timestamp;

    private boolean isSettled = false;

    public ExpenseData(long id, long eventId, String name, double amount, String payerId) {
        this.expenseId = id;
        this.eventId = eventId;
        this.name = name;
        this.amount = amount;
        this.payerId = payerId;
        this.beneficiaryIds.add(payerId);
        this.timestamp = System.currentTimeMillis();
    }

    public long getEventId() { return eventId; }
    public String getName() { return name; }
    public long getTimestamp() { return timestamp; }
    public boolean isSettled() { return isSettled; }
    public void setSettled() { isSettled = true; }

    @Override
    public void setId(long id) {
        expenseId = id;
    }

    public EmbedBuilder createEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.ORANGE);
        embed.setTitle("Expense Details");
        embed.setTimestamp(Instant.ofEpochMilli(timestamp));

        embed.setDescription("### " + name);

        double share = beneficiaryIds.isEmpty()
                ? 0.0
                : amount / beneficiaryIds .size();

        embed.addField("Total Amount", CurrencyUtils.formatAsUSD(amount), true);
        embed.addField("Paid By", "<@" + payerId + ">", true);
        embed.addBlankField(true);
        embed.addField("Share per Person", CurrencyUtils.formatAsUSD(share), true);
        embed.addField("Beneficiaries", String.valueOf(beneficiaryIds.size()), true);
        embed.addBlankField(true);
        embed.addField("Expense ID", "`" + expenseId + "`", false);

        return embed;
    }
}
