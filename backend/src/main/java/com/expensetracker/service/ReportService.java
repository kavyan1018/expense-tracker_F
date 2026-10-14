package com.expensetracker.service;

import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.ReportDtos.CategoryTotal;
import com.expensetracker.dto.ReportDtos.MonthTotal;
import com.expensetracker.dto.ReportDtos.Summary;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactions;
    private final Clock clock;

    public ReportService(TransactionRepository transactions, Clock clock) {
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Summary summary(Long userId, LocalDate from, LocalDate to) {
        checkRange(from, to);
        BigDecimal income = transactions.sumByType(userId, TxnType.INCOME, from, to);
        BigDecimal expense = transactions.sumByType(userId, TxnType.EXPENSE, from, to);
        return new Summary(from, to, income, expense, income.subtract(expense));
    }

    @Transactional(readOnly = true)
    public List<CategoryTotal> byCategory(Long userId, LocalDate from, LocalDate to, TxnType type) {
        checkRange(from, to);
        List<Object[]> rows = transactions.totalsByCategory(userId, type, from, to);
        BigDecimal grand = rows.stream().map(r -> (BigDecimal) r[3]).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream().map(r -> {
            BigDecimal total = (BigDecimal) r[3];
            double percent = grand.signum() == 0 ? 0
                    : total.multiply(BigDecimal.valueOf(100)).divide(grand, 1, RoundingMode.HALF_UP).doubleValue();
            return new CategoryTotal((Long) r[0], (String) r[1], (String) r[2], total, percent);
        }).toList();
    }

    /** Income and expense for each of the last {@code months} months, including the current one, oldest first. */
    @Transactional(readOnly = true)
    public List<MonthTotal> monthlyTrend(Long userId, int months) {
        int n = Math.min(Math.max(months, 1), 36);
        YearMonth last = YearMonth.now(clock);
        YearMonth first = last.minusMonths(n - 1L);
        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            totals.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] r : transactions.monthlyTotals(userId, first.atDay(1), last.atEndOfMonth())) {
            YearMonth m = YearMonth.of(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            BigDecimal[] pair = totals.get(m);
            if (pair != null) {
                pair[r[2] == TxnType.INCOME ? 0 : 1] = (BigDecimal) r[3];
            }
        }
        List<MonthTotal> out = new ArrayList<>();
        totals.forEach((m, pair) -> out.add(new MonthTotal(m.toString(), pair[0], pair[1])));
        return out;
    }

    static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Both from and to dates are required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("The end date must be on or after the start date");
        }
    }
}
