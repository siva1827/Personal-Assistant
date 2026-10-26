package com.siva.assistant.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.siva.assistant.user.DietaryPreference;
import java.time.Instant;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        Integer cookTimeMinutes,
        Integer servings,
        String cuisine,
        DietaryPreference dietaryType,
        JsonNode ingredients,
        JsonNode missingIngredients,
        JsonNode steps,
        boolean favorite,
        Instant createdAt) {
}
