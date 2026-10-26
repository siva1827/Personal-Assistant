package com.siva.assistant.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.expense.ExpenseDtos.CreateExpenseRequest;
import com.siva.assistant.expense.ExpenseDtos.UpdateExpenseRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final long USER = 7L;

    @Mock
    ExpenseRepository expenses;
    @Mock
    CategoryRepository categories;

    ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(expenses, categories);
    }

    private static Category category(long id, String name) {
        Category c = new Category(USER, name, "x");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void createRejectsCategoryOfAnotherUser() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER,
                new CreateExpenseRequest(1L, new BigDecimal("10"), null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(expenses, never()).save(any());
    }

    @Test
    void createDefaultsDateToTodayAndTrimsNote() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(1L, "Food")));
        when(expenses.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(USER, new CreateExpenseRequest(1L, new BigDecimal("12.50"), "  lunch ", null));

        assertThat(response.spentOn()).isEqualTo(LocalDate.now());
        assertThat(response.note()).isEqualTo("lunch");
        assertThat(response.source()).isEqualTo(ExpenseSource.MANUAL);
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndBlankNoteClears() {
        Expense expense = new Expense(USER, category(1L, "Food"), new BigDecimal("10"), "old",
                LocalDate.of(2026, 10, 1), ExpenseSource.MANUAL);
        when(expenses.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(expense));

        service.update(USER, 5L, new UpdateExpenseRequest(null, new BigDecimal("20"), "  ", null));

        assertThat(expense.getAmount()).isEqualByComparingTo("20");
        assertThat(expense.getNote()).isNull();
        assertThat(expense.getSpentOn()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(expense.getCategory().getName()).isEqualTo("Food");
    }

    @Test
    void otherUsersExpenseIsNotFound() {
        when(expenses.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER, 5L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
