package com.siva.assistant.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The JSON shape every recipe reply must have, written in Gemini's response-schema dialect. */
public final class RecipeSchema {

    private RecipeSchema() {
    }

    public static Map<String, Object> gemini() {
        Map<String, Object> ingredient = obj(
                Map.of("name", type("STRING"), "quantity", type("STRING"), "unit", type("STRING")),
                List.of("name"));

        Map<String, Object> diet = new LinkedHashMap<>();
        diet.put("type", "STRING");
        diet.put("enum", List.of("VEG", "NON_VEG", "VEGAN", "EGGETARIAN"));

        Map<String, Object> recipeProps = new LinkedHashMap<>();
        recipeProps.put("title", type("STRING"));
        recipeProps.put("description", type("STRING"));
        recipeProps.put("cookTimeMinutes", type("INTEGER"));
        recipeProps.put("servings", type("INTEGER"));
        recipeProps.put("cuisine", type("STRING"));
        recipeProps.put("dietaryType", diet);
        recipeProps.put("ingredients", array(ingredient));
        recipeProps.put("steps", array(type("STRING")));

        Map<String, Object> recipe = obj(recipeProps,
                List.of("title", "cookTimeMinutes", "servings", "dietaryType", "ingredients", "steps"));
        return obj(Map.of("recipes", array(recipe)), List.of("recipes"));
    }

    private static Map<String, Object> type(String type) {
        return Map.of("type", type);
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        return Map.of("type", "ARRAY", "items", items);
    }

    private static Map<String, Object> obj(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
}
