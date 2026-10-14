package com.expensetracker.web;

import com.expensetracker.dto.BudgetDtos.BudgetRequest;
import com.expensetracker.dto.BudgetDtos.BudgetResponse;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgets;

    public BudgetController(BudgetService budgets) {
        this.budgets = budgets;
    }

    @GetMapping
    public List<BudgetResponse> forMonth(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgets.forMonth(CurrentUser.id(), month);
    }

    @PutMapping
    public BudgetResponse upsert(@Valid @RequestBody BudgetRequest req) {
        return budgets.upsert(CurrentUser.id(), req);
    }

    @PostMapping("/copy")
    public List<BudgetResponse> copy(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return budgets.copy(CurrentUser.id(), from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        budgets.delete(CurrentUser.id(), id);
    }
}
