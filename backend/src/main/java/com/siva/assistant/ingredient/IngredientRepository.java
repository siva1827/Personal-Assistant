package com.siva.assistant.ingredient;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByNameContainingIgnoreCaseOrderByName(String query, Pageable pageable);
}
