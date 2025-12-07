package dev.supersand24.expenses;

import dev.supersand24.IData;

import java.util.ArrayList;
import java.util.List;

public class ExpenseData implements IData {

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
    public void setEventId(long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public List<String> getBeneficiaryIds() { return beneficiaryIds; }
    public void addBeneficiaryId(String beneficiaryId) {
        if (!beneficiaryIds.contains(beneficiaryId))
            beneficiaryIds.add(beneficiaryId);
    }
    public void removeBeneficiaryId(String beneficiaryId) { beneficiaryIds.remove(beneficiaryId); }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSettled() { return isSettled; }
    public void setSettled() { isSettled = true; }

}
