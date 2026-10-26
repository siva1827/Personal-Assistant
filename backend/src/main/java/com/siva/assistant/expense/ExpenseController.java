package com.siva.assistant.expense;

import com.siva.assistant.common.PageResponse;
import com.siva.assistant.expense.ExpenseDtos.CreateExpenseRequest;
import com.siva.assistant.expense.ExpenseDtos.ExpenseResponse;
import com.siva.assistant.expense.ExpenseDtos.MonthSummary;
import com.siva.assistant.expense.ExpenseDtos.UpdateExpenseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
@Validated
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SpendingSummaryService summaryService;

    public ExpenseController(ExpenseService expenseService, SpendingSummaryService summaryService) {
        this.expenseService = expenseService;
        this.summaryService = summaryService;
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return expenseService.list(userId, orCurrent(month), page, size);
    }

    @GetMapping("/summary")
    public MonthSummary summary(@AuthenticationPrincipal Long userId,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return summaryService.summary(userId, orCurrent(month));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(userId, request);
    }

    @PatchMapping("/{id}")
    public ExpenseResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @Valid @RequestBody UpdateExpenseRequest request) {
        return expenseService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        expenseService.delete(userId, id);
    }

    private static YearMonth orCurrent(YearMonth month) {
        return month != null ? month : YearMonth.now();
    }
}
