package com.expensetracker.service;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.PaymentMethod;
import com.expensetracker.domain.Transaction;
import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.TransactionDtos.PageResponse;
import com.expensetracker.dto.TransactionDtos.TransactionRequest;
import com.expensetracker.dto.TransactionDtos.TransactionResponse;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactions, CategoryService categoryService) {
        this.transactions = transactions;
        this.categoryService = categoryService;
    }

    public record Filter(LocalDate from, LocalDate to, TxnType type, Long categoryId, PaymentMethod paymentMethod, String q) {
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(Long userId, Filter f, int page, int size) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("txnDate"), f.from()));
            if (f.to() != null) p.add(cb.lessThanOrEqualTo(root.get("txnDate"), f.to()));
            if (f.type() != null) p.add(cb.equal(root.get("type"), f.type()));
            if (f.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.paymentMethod() != null) p.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
            if (f.q() != null && !f.q().isBlank()) {
                p.add(cb.like(cb.lower(root.get("note")), "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("txnDate"), Sort.Order.desc("id")));
        return PageResponse.of(transactions.findAll(spec, pageable), TransactionResponse::of);
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest req) {
        Category category = categoryService.getUsable(userId, req.categoryId());
        Transaction t = new Transaction(userId, category, req.amount(), req.date(), trim(req.note()),
                req.paymentMethod(), null);
        return TransactionResponse.of(transactions.save(t));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest req) {
        Transaction t = getOwned(userId, id);
        if (!t.getCategory().getId().equals(req.categoryId())) {
            t.setCategory(categoryService.getUsable(userId, req.categoryId()));
        }
        t.setAmount(req.amount());
        t.setTxnDate(req.date());
        t.setNote(trim(req.note()));
        t.setPaymentMethod(req.paymentMethod());
        return TransactionResponse.of(transactions.saveAndFlush(t));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        transactions.delete(getOwned(userId, id));
    }

    private Transaction getOwned(Long userId, Long id) {
        return transactions.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Transaction"));
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
