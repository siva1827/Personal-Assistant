package com.siva.assistant.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    /**
     * spentOn defaults to today and source to MANUAL. Imports from a bank SMS send source SMS and an
     * externalRef (a fingerprint of the message); the same externalRef is only accepted once.
     */
    public record CreateExpenseRequest(
            @NotNull Long categoryId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 200) String note,
            LocalDate spentOn,
            ExpenseSource source,
            @Size(max = 64) String externalRef) {
    }

    /** Fields left null are not changed; a blank note clears it. */
    public record UpdateExpenseRequest(
            Long categoryId,
            @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 200) String note,
            LocalDate spentOn) {
    }

    public record ExpenseResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryEmoji,
            BigDecimal amount,
            String note,
            LocalDate spentOn,
            ExpenseSource source) {

        static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getCategory().getId(), e.getCategory().getName(),
                    e.getCategory().getEmoji(), e.getAmount(), e.getNote(), e.getSpentOn(), e.getSource());
        }
    }

    public enum BudgetStatus {
        OK, NEAR, OVER
    }

    /** budget and status are null when the category has no budget this month. */
    public record CategorySummary(
            Long categoryId,
            String name,
            String emoji,
            BigDecimal total,
            BigDecimal budget,
            BudgetStatus status) {
    }

    public record DaySummary(LocalDate date, BigDecimal total) {
    }

    public record MonthSummary(
            YearMonth month,
            BigDecimal total,
            List<CategorySummary> byCategory,
            List<DaySummary> byDay) {
    }
}
