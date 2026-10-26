package com.siva.assistant.budget;

import com.siva.assistant.budget.BudgetDtos.BudgetResponse;
import com.siva.assistant.budget.BudgetDtos.SetBudgetRequest;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal Long userId,
                                     @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.list(userId, orCurrent(month));
    }

    @PutMapping("/{categoryId}")
    public BudgetResponse set(@AuthenticationPrincipal Long userId, @PathVariable Long categoryId,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                              @Valid @RequestBody SetBudgetRequest request) {
        return budgetService.set(userId, categoryId, orCurrent(month), request.limitAmount());
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal Long userId, @PathVariable Long categoryId,
                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        budgetService.clear(userId, categoryId, orCurrent(month));
    }

    private static YearMonth orCurrent(YearMonth month) {
        return month != null ? month : YearMonth.now();
    }
}
