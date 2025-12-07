package dev.supersand24.expenses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpenseDataStore {

    private long nextExpenseId = 1;

    private Map<Long, ExpenseData> expenses = new ConcurrentHashMap<>();

    private Map<String, List<PaymentInfo>> paymentDetails = new ConcurrentHashMap<>();

    private Map<Long, DebtData> debts = new ConcurrentHashMap<>();
    private long nextDebtId = 1;

    public long getAndIncrementNextExpenseId() {
        synchronized (this) {
            return nextExpenseId++;
        }
    }

    public long getAndIncrementNextDebtId() {
        synchronized (this) {
            return nextDebtId++;
        }
    }

    public Map<Long, ExpenseData> getExpenses() { return expenses; }
    public Map<String, List<PaymentInfo>> getPaymentDetails() { return paymentDetails; }
    public Map<Long, DebtData> getDebts() { return debts; }

}
