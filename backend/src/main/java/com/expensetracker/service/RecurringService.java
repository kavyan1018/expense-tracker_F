package com.expensetracker.service;

import com.expensetracker.domain.RecurringExpense;
import com.expensetracker.domain.Transaction;
import com.expensetracker.dto.RecurringDtos.RecurringRequest;
import com.expensetracker.dto.RecurringDtos.RecurringResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.RecurringExpenseRepository;
import com.expensetracker.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringService {

    private static final Logger log = LoggerFactory.getLogger(RecurringService.class);

    private final RecurringExpenseRepository rules;
    private final TransactionRepository transactions;
    private final CategoryService categoryService;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RecurringService(RecurringExpenseRepository rules, TransactionRepository transactions,
                            CategoryService categoryService, TransactionTemplate tx, Clock clock) {
        this.rules = rules;
        this.transactions = transactions;
        this.categoryService = categoryService;
        this.tx = tx;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecurringResponse> list(Long userId) {
        return rules.findAllForUser(userId).stream().map(RecurringResponse::of).toList();
    }

    /** Creates a rule. Occurrences from the start date up to today are posted immediately. */
    @Transactional
    public RecurringResponse create(Long userId, RecurringRequest req) {
        RecurringExpense rule = new RecurringExpense(userId);
        apply(userId, rule, req);
        rule.setNextRunDate(req.startDate());
        rules.save(rule);
        postDue(rule, today());
        return RecurringResponse.of(rule);
    }

    /** Updates a rule. Already-posted transactions stay as they are; the new schedule applies from today. */
    @Transactional
    public RecurringResponse update(Long userId, Long id, RecurringRequest req) {
        RecurringExpense rule = getOwned(userId, id);
        apply(userId, rule, req);
        rule.setNextRunDate(firstOnOrAfter(rule, today()));
        rule.setActive(true);
        postDue(rule, today());
        return RecurringResponse.of(rule);
    }

    @Transactional
    public RecurringResponse pause(Long userId, Long id) {
        RecurringExpense rule = getOwned(userId, id);
        rule.setActive(false);
        return RecurringResponse.of(rule);
    }

    /** Resumes from today; occurrences missed while paused are not posted. */
    @Transactional
    public RecurringResponse resume(Long userId, Long id) {
        RecurringExpense rule = getOwned(userId, id);
        rule.setActive(true);
        rule.setNextRunDate(firstOnOrAfter(rule, today()));
        postDue(rule, today());
        return RecurringResponse.of(rule);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        // posted transactions are kept; their recurring_id is cleared by the foreign key
        rules.delete(getOwned(userId, id));
    }

    /** Posts every due occurrence for every user. Runs daily and once at startup. */
    @Scheduled(cron = "${app.recurring.cron}")
    @EventListener(ApplicationReadyEvent.class)
    public void postAllDue() {
        LocalDate today = today();
        int posted = 0;
        for (Long id : rules.findDueIds(today)) {
            try {
                Integer n = tx.execute(status -> rules.findById(id).map(r -> postDue(r, today)).orElse(0));
                posted += n == null ? 0 : n;
            } catch (RuntimeException e) {
                log.error("Could not post recurring expense {}", id, e);
            }
        }
        if (posted > 0) {
            log.info("Posted {} recurring transaction(s)", posted);
        }
    }

    /** Posts occurrences up to and including {@code today}, then moves the rule to its next date. */
    int postDue(RecurringExpense rule, LocalDate today) {
        int posted = 0;
        while (rule.isActive() && !rule.getNextRunDate().isAfter(today)) {
            LocalDate date = rule.getNextRunDate();
            if (rule.getEndDate() != null && date.isAfter(rule.getEndDate())) {
                break;
            }
            if (!transactions.existsByRecurringIdAndTxnDate(rule.getId(), date)) {
                transactions.save(new Transaction(rule.getUserId(), rule.getCategory(), rule.getAmount(), date,
                        rule.getDescription(), rule.getId()));
                posted++;
            }
            rule.setNextRunDate(rule.getFrequency().nextAfter(rule.getStartDate(), date));
        }
        if (rule.getEndDate() != null && rule.getNextRunDate().isAfter(rule.getEndDate())) {
            rule.setActive(false);
        }
        return posted;
    }

    private void apply(Long userId, RecurringExpense rule, RecurringRequest req) {
        if (req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
        if (rule.getCategory() == null || !rule.getCategory().getId().equals(req.categoryId())) {
            rule.setCategory(categoryService.getUsable(userId, req.categoryId()));
        }
        rule.setAmount(req.amount());
        rule.setDescription(req.description().trim());
        rule.setFrequency(req.frequency());
        rule.setStartDate(req.startDate());
        rule.setEndDate(req.endDate());
    }

    private static LocalDate firstOnOrAfter(RecurringExpense rule, LocalDate date) {
        return rule.getFrequency().nextAfter(rule.getStartDate(), date.minusDays(1));
    }

    private RecurringExpense getOwned(Long userId, Long id) {
        return rules.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Recurring expense"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
