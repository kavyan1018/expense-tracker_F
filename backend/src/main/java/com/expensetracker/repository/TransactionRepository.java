package com.expensetracker.repository;

import com.expensetracker.domain.Transaction;
import com.expensetracker.domain.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByRecurringIdAndTxnDate(Long recurringId, LocalDate txnDate);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.type = :type and t.txnDate between :from and :to""")
    BigDecimal sumByType(@Param("userId") Long userId, @Param("type") TxnType type,
                         @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.category.id = :categoryId and t.txnDate between :from and :to""")
    BigDecimal sumByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Rows of [categoryId, categoryName, color, total], largest first. */
    @Query("""
            select c.id, c.name, c.color, sum(t.amount) from Transaction t join t.category c
            where t.userId = :userId and t.type = :type and t.txnDate between :from and :to
            group by c.id, c.name, c.color
            order by sum(t.amount) desc""")
    List<Object[]> totalsByCategory(@Param("userId") Long userId, @Param("type") TxnType type,
                                    @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Rows of [year, month, type, total]. */
    @Query("""
            select year(t.txnDate), month(t.txnDate), t.type, sum(t.amount) from Transaction t
            where t.userId = :userId and t.txnDate between :from and :to
            group by year(t.txnDate), month(t.txnDate), t.type""")
    List<Object[]> monthlyTotals(@Param("userId") Long userId,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select t from Transaction t join fetch t.category
            where t.userId = :userId and t.txnDate between :from and :to
            order by t.txnDate desc, t.id desc""")
    List<Transaction> findForExport(@Param("userId") Long userId,
                                    @Param("from") LocalDate from, @Param("to") LocalDate to);
}
