package com.expensetracker.repository;

import com.expensetracker.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    @Query("select b from Budget b left join fetch b.category where b.userId = :userId and b.periodMonth = :month")
    List<Budget> findForMonth(@Param("userId") Long userId, @Param("month") LocalDate month);

    Optional<Budget> findByUserIdAndPeriodMonthAndCategoryIsNull(Long userId, LocalDate month);

    Optional<Budget> findByUserIdAndPeriodMonthAndCategoryId(Long userId, LocalDate month, Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
