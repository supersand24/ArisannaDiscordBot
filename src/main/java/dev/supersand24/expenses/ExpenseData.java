package dev.supersand24.expenses;

import dev.supersand24.CurrencyUtils;
import dev.supersand24.Identifiable;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExpenseData implements Identifiable {

    private transient long expenseId;
    private long eventId;
    private String name;
    private double amount;
    private String payerId;
    private List<String> beneficiaryIds = new ArrayList<>();
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

    public long getId() { return expenseId; }
    @Override public void setId(long id) {
        expenseId = id;
    }
    public long getEventId() { return eventId; }
    public String getName() { return name; }
    public double getAmount() { return amount; }
    public String getPayerId() { return payerId; }
    public List<String> getBeneficiaryIds() { return beneficiaryIds; }
    public long getTimestamp() { return timestamp; }
    public boolean isSettled() { return isSettled; }
    public void setSettled() { isSettled = true; }

}
