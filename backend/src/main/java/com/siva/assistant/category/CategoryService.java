package com.siva.assistant.category;

import com.siva.assistant.category.CategoryDtos.CategoryResponse;
import com.siva.assistant.category.CategoryDtos.CreateCategoryRequest;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.expense.ExpenseRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    /** Created for a user the first time they open their categories. */
    static final Map<String, String> DEFAULTS = Map.of(
            "Food", "🍽️", "Groceries", "🛒", "Transport", "🚌", "Bills", "🧾",
            "Shopping", "🛍️", "Health", "💊", "Entertainment", "🎬", "Other", "📦");

    private final CategoryRepository categories;
    private final ExpenseRepository expenses;

    public CategoryService(CategoryRepository categories, ExpenseRepository expenses) {
        this.categories = categories;
        this.expenses = expenses;
    }

    @Transactional
    public List<CategoryResponse> list(long userId) {
        if (categories.countByUserId(userId) == 0) {
            DEFAULTS.forEach((name, emoji) -> categories.save(new Category(userId, name, emoji)));
        }
        return categories.findByUserIdOrderByName(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(long userId, CreateCategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category \"" + name + "\" already exists");
        }
        String emoji = request.emoji() == null || request.emoji().isBlank() ? null : request.emoji().trim();
        return CategoryResponse.from(categories.save(new Category(userId, name, emoji)));
    }

    @Transactional
    public void delete(long userId, long categoryId) {
        Category category = categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (expenses.existsByCategoryId(categoryId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category has expenses; move or delete them first");
        }
        categories.delete(category);
    }
}
