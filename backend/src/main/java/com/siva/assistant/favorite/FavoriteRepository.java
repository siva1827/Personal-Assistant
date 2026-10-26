package com.siva.assistant.favorite;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<FavoriteRecipe, FavoriteId> {

    @Query("select f.id.recipeId from FavoriteRecipe f where f.id.userId = :userId and f.id.recipeId in :recipeIds")
    Set<Long> findFavoriteIds(@Param("userId") Long userId, @Param("recipeIds") Collection<Long> recipeIds);
}
