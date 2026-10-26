package com.siva.assistant.budget;

import com.siva.assistant.budget.BudgetDtos.BudgetResponse;
import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.common.ApiException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final CategoryRepository categories;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories) {
        this.budgets = budgets;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(long userId, YearMonth month) {
        Map<Long, Category> byId = categories.findByUserIdOrderByName(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return budgets.findByUserIdAndMonth(userId, month.atDay(1)).stream()
                .filter(b -> byId.containsKey(b.getCategoryId()))
                .map(b -> toResponse(b, byId.get(b.getCategoryId()), month))
                .sorted(java.util.Comparator.comparing(BudgetResponse::categoryName))
                .toList();
    }

    /** Category id to budget limit for the month; used by the spending summary. */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> limitsFor(long userId, YearMonth month) {
        return budgets.findByUserIdAndMonth(userId, month.atDay(1)).stream()
                .collect(Collectors.toMap(Budget::getCategoryId, Budget::getLimitAmount));
    }

    /** Creates the budget or replaces the limit if one already exists for that month. */
    @Transactional
    public BudgetResponse set(long userId, long categoryId, YearMonth month, BigDecimal limit) {
        Category category = findCategory(userId, categoryId);
        Budget budget = budgets.findByUserIdAndCategoryIdAndMonth(userId, categoryId, month.atDay(1))
                .orElseGet(() -> new Budget(userId, categoryId, month.atDay(1), limit));
        budget.setLimitAmount(limit);
        return toResponse(budgets.save(budget), category, month);
    }

    @Transactional
    public void clear(long userId, long categoryId, YearMonth month) {
        findCategory(userId, categoryId);
        budgets.findByUserIdAndCategoryIdAndMonth(userId, categoryId, month.atDay(1)).ifPresent(budgets::delete);
    }

    private Category findCategory(long userId, long categoryId) {
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private static BudgetResponse toResponse(Budget b, Category c, YearMonth month) {
        return new BudgetResponse(c.getId(), c.getName(), c.getEmoji(), month, b.getLimitAmount());
    }
}
