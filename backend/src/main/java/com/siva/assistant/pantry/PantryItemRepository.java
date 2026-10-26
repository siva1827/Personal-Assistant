package com.siva.assistant.pantry;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<PantryItem> findByUserIdOrderByAddedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "ingredient")
    Optional<PantryItem> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "ingredient")
    List<PantryItem> findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDate(Long userId, LocalDate cutoff);

    boolean existsByUserIdAndIngredientId(Long userId, Long ingredientId);

    @Query("select p.ingredient.id from PantryItem p where p.userId = :userId and p.ingredient.id in :ids")
    Set<Long> findExistingIngredientIds(@Param("userId") Long userId, @Param("ids") Set<Long> ids);
}
