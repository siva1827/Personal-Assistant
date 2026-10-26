package com.siva.assistant.expense;

import com.siva.assistant.budget.BudgetService;
import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.expense.ExpenseDtos.BudgetStatus;
import com.siva.assistant.expense.ExpenseDtos.CategorySummary;
import com.siva.assistant.expense.ExpenseDtos.DaySummary;
import com.siva.assistant.expense.ExpenseDtos.MonthSummary;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpendingSummaryService {

    /** Spending at or above this share of the budget counts as "near the limit". */
    static final BigDecimal NEAR_THRESHOLD = new BigDecimal("0.80");

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final BudgetService budgets;

    public SpendingSummaryService(ExpenseRepository expenses, CategoryRepository categories,
                                  BudgetService budgets) {
        this.expenses = expenses;
        this.categories = categories;
        this.budgets = budgets;
    }

    @Transactional(readOnly = true)
    public MonthSummary summary(long userId, YearMonth month) {
        Map<Long, BigDecimal> spent = expenses.totalsByCategory(userId, month.atDay(1), month.atEndOfMonth())
                .stream().collect(Collectors.toMap(CategoryTotal::categoryId, CategoryTotal::total));
        Map<Long, BigDecimal> limits = budgets.limitsFor(userId, month);
        Map<Long, Category> byId = categories.findByUserIdOrderByName(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        // Show categories that have spending or a budget this month, biggest spend first
        List<CategorySummary> rows = byId.values().stream()
                .filter(c -> spent.containsKey(c.getId()) || limits.containsKey(c.getId()))
                .map(c -> {
                    BigDecimal total = spent.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal limit = limits.get(c.getId());
                    return new CategorySummary(c.getId(), c.getName(), c.getEmoji(), total, limit,
                            statusFor(total, limit));
                })
                .sorted(Comparator.comparing(CategorySummary::total).reversed()
                        .thenComparing(CategorySummary::name))
                .toList();

        BigDecimal total = spent.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DaySummary> byDay = expenses.totalsByDay(userId, month.atDay(1), month.atEndOfMonth()).stream()
                .map(d -> new DaySummary(d.date(), d.total())).toList();
        return new MonthSummary(month, total, rows, byDay);
    }

    /** Null when there is no budget. */
    static BudgetStatus statusFor(BigDecimal spent, BigDecimal limit) {
        if (limit == null) {
            return null;
        }
        if (spent.compareTo(limit) >= 0) {
            return BudgetStatus.OVER;
        }
        if (spent.compareTo(limit.multiply(NEAR_THRESHOLD)) >= 0) {
            return BudgetStatus.NEAR;
        }
        return BudgetStatus.OK;
    }
}
