package com.siva.assistant.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = "category")
    Page<Expense> findByUserIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(
            Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByUserIdAndExternalRef(Long userId, String externalRef);

    /** Which of the given import fingerprints this user already has; one query for a whole batch. */
    @Query("select e.externalRef from Expense e where e.userId = :userId and e.externalRef in :refs")
    List<String> findExistingExternalRefs(@Param("userId") Long userId,
                                          @Param("refs") java.util.Collection<String> refs);

    @Query("""
            select new com.siva.assistant.expense.CategoryTotal(e.category.id, sum(e.amount))
            from Expense e
            where e.userId = :userId and e.spentOn between :from and :to
            group by e.category.id
            """)
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId, @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    @Query("""
            select new com.siva.assistant.expense.DayTotal(e.spentOn, sum(e.amount))
            from Expense e
            where e.userId = :userId and e.spentOn between :from and :to
            group by e.spentOn
            order by e.spentOn
            """)
    List<DayTotal> totalsByDay(@Param("userId") Long userId, @Param("from") LocalDate from,
                               @Param("to") LocalDate to);
}
