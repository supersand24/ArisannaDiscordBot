package dev.supersand24.expenses;

public class Debt {

    private final long debtId;
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

}
