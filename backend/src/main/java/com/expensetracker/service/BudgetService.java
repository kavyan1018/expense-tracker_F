package com.expensetracker.service;

import com.expensetracker.domain.Budget;
import com.expensetracker.domain.Category;
import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.BudgetDtos.BudgetRequest;
import com.expensetracker.dto.BudgetDtos.BudgetResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final CategoryService categoryService;

    public BudgetService(BudgetRepository budgets, TransactionRepository transactions,
                         CategoryService categoryService) {
        this.budgets = budgets;
        this.transactions = transactions;
        this.categoryService = categoryService;
    }

    /** Budgets for the month with what has been spent so far. The overall budget comes first. */
    @Transactional(readOnly = true)
    public List<BudgetResponse> forMonth(Long userId, YearMonth month) {
        return budgets.findForMonth(userId, month.atDay(1)).stream()
                .map(b -> toResponse(userId, b))
                .sorted(Comparator.comparing((BudgetResponse r) -> r.categoryId() != null)
                        .thenComparing(r -> r.categoryName() == null ? "" : r.categoryName()))
                .toList();
    }

    /** Creates the budget, or updates its amount if one already exists for that month and category. */
    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest req) {
        LocalDate month = req.month().atDay(1);
        Budget budget;
        if (req.categoryId() == null) {
            budget = budgets.findByUserIdAndPeriodMonthAndCategoryIsNull(userId, month)
                    .orElseGet(() -> new Budget(userId, null, month, req.amount()));
        } else {
            Category category = categoryService.getUsable(userId, req.categoryId());
            if (category.getType() != TxnType.EXPENSE) {
                throw new BadRequestException("Budgets can only be set on expense categories");
            }
            budget = budgets.findByUserIdAndPeriodMonthAndCategoryId(userId, month, category.getId())
                    .orElseGet(() -> new Budget(userId, category, month, req.amount()));
        }
        budget.setAmount(req.amount());
        return toResponse(userId, budgets.save(budget));
    }

    /** Copies every budget from one month into another, skipping ones the target month already has. */
    @Transactional
    public List<BudgetResponse> copy(Long userId, YearMonth from, YearMonth to) {
        for (Budget b : budgets.findForMonth(userId, from.atDay(1))) {
            Long categoryId = b.getCategory() == null ? null : b.getCategory().getId();
            if (b.getCategory() != null && b.getCategory().isArchived()) {
                continue;
            }
            boolean exists = categoryId == null
                    ? budgets.findByUserIdAndPeriodMonthAndCategoryIsNull(userId, to.atDay(1)).isPresent()
                    : budgets.findByUserIdAndPeriodMonthAndCategoryId(userId, to.atDay(1), categoryId).isPresent();
            if (!exists) {
                budgets.save(new Budget(userId, b.getCategory(), to.atDay(1), b.getAmount()));
            }
        }
        return forMonth(userId, to);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Budget")));
    }

    private BudgetResponse toResponse(Long userId, Budget b) {
        YearMonth month = YearMonth.from(b.getPeriodMonth());
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Category c = b.getCategory();
        BigDecimal spent = c == null
                ? transactions.sumByType(userId, TxnType.EXPENSE, from, to)
                : transactions.sumByCategory(userId, c.getId(), from, to);
        int percent = spent.multiply(BigDecimal.valueOf(100))
                .divide(b.getAmount(), 0, RoundingMode.HALF_UP).intValue();
        return new BudgetResponse(b.getId(), month, c == null ? null : c.getId(), c == null ? null : c.getName(),
                c == null ? null : c.getColor(), b.getAmount(), spent, b.getAmount().subtract(spent), percent);
    }
}
