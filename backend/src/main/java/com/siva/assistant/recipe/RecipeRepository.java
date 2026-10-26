package com.siva.assistant.recipe;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByIdAndUserId(Long id, Long userId);

    Page<Recipe> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            select r from Recipe r, FavoriteRecipe f
            where f.id.recipeId = r.id and f.id.userId = :userId
            order by f.savedAt desc, r.id desc
            """)
    Page<Recipe> findFavorites(@Param("userId") Long userId, Pageable pageable);
}
