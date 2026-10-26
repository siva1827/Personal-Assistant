package com.siva.assistant.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.siva.assistant.budget.BudgetService;
import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.expense.ExpenseDtos.BudgetStatus;
import com.siva.assistant.expense.ExpenseDtos.MonthSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpendingSummaryServiceTest {

    private static final long USER = 7L;
    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Mock
    ExpenseRepository expenses;
    @Mock
    CategoryRepository categories;
    @Mock
    BudgetService budgets;

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static Category category(long id, String name) {
        Category c = new Category(USER, name, "x");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void statusThresholds() {
        assertThat(SpendingSummaryService.statusFor(d("10"), null)).isNull();
        assertThat(SpendingSummaryService.statusFor(d("79.99"), d("100"))).isEqualTo(BudgetStatus.OK);
        assertThat(SpendingSummaryService.statusFor(d("80"), d("100"))).isEqualTo(BudgetStatus.NEAR);
        assertThat(SpendingSummaryService.statusFor(d("99.99"), d("100"))).isEqualTo(BudgetStatus.NEAR);
        assertThat(SpendingSummaryService.statusFor(d("100"), d("100"))).isEqualTo(BudgetStatus.OVER);
    }

    @Test
    void summaryCombinesSpendingAndBudgets() {
        LocalDate from = MONTH.atDay(1);
        LocalDate to = MONTH.atEndOfMonth();
        when(expenses.totalsByCategory(USER, from, to))
                .thenReturn(List.of(new CategoryTotal(1L, d("250.00")), new CategoryTotal(2L, d("40.50"))));
        when(expenses.totalsByDay(USER, from, to))
                .thenReturn(List.of(new DayTotal(LocalDate.of(2026, 10, 3), d("290.50"))));
        // Category 3 has a budget but no spending; category 4 has neither and is hidden
        when(budgets.limitsFor(USER, MONTH)).thenReturn(Map.of(1L, d("300.00"), 3L, d("500.00")));
        when(categories.findByUserIdOrderByName(USER)).thenReturn(List.of(
                category(1L, "Food"), category(2L, "Transport"), category(3L, "Bills"), category(4L, "Other")));

        MonthSummary summary = new SpendingSummaryService(expenses, categories, budgets).summary(USER, MONTH);

        assertThat(summary.total()).isEqualByComparingTo("290.50");
        assertThat(summary.byCategory()).extracting("name").containsExactly("Food", "Transport", "Bills");
        assertThat(summary.byCategory().get(0).status()).isEqualTo(BudgetStatus.NEAR);
        assertThat(summary.byCategory().get(1).status()).isNull();
        assertThat(summary.byCategory().get(2).total()).isEqualByComparingTo("0");
        assertThat(summary.byCategory().get(2).status()).isEqualTo(BudgetStatus.OK);
        assertThat(summary.byDay()).hasSize(1);
    }
}
