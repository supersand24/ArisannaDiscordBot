package dev.supersand24.expenses;

import java.util.List;

/**
 * A simple data object to hold the results of a settlement calculation.
 * @param newDebts The list of newly generated debts.
 * @param expensesProcessedCount The number of expenses included in this calculation.
 */
public record SettlementResult(List<DebtData> newDebts, int expensesProcessedCount) {
}