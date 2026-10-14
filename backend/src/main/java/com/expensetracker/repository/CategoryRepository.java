package com.expensetracker.repository;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    List<Category> findByUserIdOrderByTypeAscNameAsc(Long userId);

    boolean existsByUserIdAndTypeAndNameIgnoreCase(Long userId, TxnType type, String name);

    boolean existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(Long userId, TxnType type, String name, Long id);
}
