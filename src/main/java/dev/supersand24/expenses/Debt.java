package dev.supersand24.expenses;

import dev.supersand24.Identifiable;

public class Debt implements Identifiable {

    private transient long debtId;
    private final String debtorId;
    private final String creditorId;
    private final double amount;
    private boolean isPaid;

    public Debt(long debtId, String debtorId, String creditorId, double amount) {
        this.debtId = debtId;
        this.debtorId = debtorId;
        this.creditorId = creditorId;
        this.amount = amount;
        this.isPaid = false;
    }

    public long getDebtId() { return debtId; }
    public String getDebtorId() { return debtorId; }
    public String getCreditorId() { return creditorId; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return isPaid; }

    public void markAsPaid() {
        this.isPaid = true;
    }

    @Override
    public void setId(long id) {
        debtId = id;
    }
}
