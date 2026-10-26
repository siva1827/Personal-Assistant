package com.siva.assistant.ingredient;

public record IngredientResponse(Long id, String name, String category, boolean staple) {

    static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getCategory(),
                ingredient.isStaple());
    }
}
