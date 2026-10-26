package com.siva.assistant.expense;

import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.common.PageResponse;
import com.siva.assistant.expense.ExpenseDtos.CreateExpenseRequest;
import com.siva.assistant.expense.ExpenseDtos.ExpenseResponse;
import com.siva.assistant.expense.ExpenseDtos.UpdateExpenseRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories) {
        this.expenses = expenses;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(long userId, YearMonth month, int page, int size) {
        return PageResponse.of(
                expenses.findByUserIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(
                        userId, month.atDay(1), month.atEndOfMonth(), PageRequest.of(page, size)),
                ExpenseResponse::from);
    }

    @Transactional
    public ExpenseResponse create(long userId, CreateExpenseRequest request) {
        Category category = findCategory(userId, request.categoryId());
        LocalDate spentOn = request.spentOn() != null ? request.spentOn() : LocalDate.now();
        Expense expense = new Expense(userId, category, request.amount(), cleanNote(request.note()), spentOn,
                ExpenseSource.MANUAL);
        return ExpenseResponse.from(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(long userId, long expenseId, UpdateExpenseRequest request) {
        Expense expense = find(userId, expenseId);
        if (request.categoryId() != null) {
            expense.setCategory(findCategory(userId, request.categoryId()));
        }
        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }
        if (request.note() != null) {
            expense.setNote(cleanNote(request.note()));
        }
        if (request.spentOn() != null) {
            expense.setSpentOn(request.spentOn());
        }
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(long userId, long expenseId) {
        expenses.delete(find(userId, expenseId));
    }

    private Expense find(long userId, long expenseId) {
        // Scoped to the user so other users' expenses look like they do not exist
        return expenses.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private Category findCategory(long userId, long categoryId) {
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private static String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
