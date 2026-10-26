package com.siva.assistant.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record SetBudgetRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal limitAmount) {
    }

    public record BudgetResponse(Long categoryId, String categoryName, String categoryEmoji, YearMonth month,
                                 BigDecimal limitAmount) {
    }
}
