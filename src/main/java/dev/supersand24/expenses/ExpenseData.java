package dev.supersand24.expenses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseData {

    public transient String expenseId;
    public String name;
    public double amount;
    public String payerId;
    public List<String> beneficiaryIds = new ArrayList<>();
    public long timestamp;

    public ExpenseData(String name, double amount, String payerId) {
        this.expenseId = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.payerId = payerId;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

}
