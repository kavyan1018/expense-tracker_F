package com.expensetracker.repository;

import com.expensetracker.domain.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    Optional<RecurringExpense> findByIdAndUserId(Long id, Long userId);

    @Query("select r from RecurringExpense r join fetch r.category where r.userId = :userId order by r.active desc, r.nextRunDate")
    List<RecurringExpense> findAllForUser(@Param("userId") Long userId);

    @Query("select r.id from RecurringExpense r where r.active = true and r.nextRunDate <= :today")
    List<Long> findDueIds(@Param("today") LocalDate today);

    boolean existsByCategoryId(Long categoryId);
}
