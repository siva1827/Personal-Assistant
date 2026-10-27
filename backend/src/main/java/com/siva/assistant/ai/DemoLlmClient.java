package com.siva.assistant.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stand-in for a real model, switched on with AI_PROVIDER=demo. It reads the pantry out of the
 * prompt and writes simple, obviously labelled recipes. It exists so the whole flow (request,
 * validation, safety checks, saving, screens) can be tried before an API key is available.
 *
 * One of the recipes always contains peanuts, so the allergy filter can be seen working for
 * anyone who lists peanuts as an allergy.
 */
public class DemoLlmClient implements LlmClient {

    private static final Pattern PANTRY = Pattern.compile("(?m)^Pantry: (.*)$");
    private static final Pattern COUNT = Pattern.compile("(?m)^Number of recipes: (\\d+)$");
    private static final Pattern DIET = Pattern.compile("(?m)^Diet: (\\w+)$");

    private final ObjectMapper mapper;

    public DemoLlmClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String modelName() {
        return "demo";
    }

    @Override
    public LlmReply generate(LlmRequest request) {
        String prompt = request.turns().get(0).text();
        List<String> pantry = new ArrayList<>();
        Matcher p = PANTRY.matcher(prompt);
        if (p.find()) {
            for (String name : p.group(1).split(",")) {
                if (!name.isBlank()) {
                    pantry.add(name.trim());
                }
            }
        }
        if (pantry.isEmpty()) {
            pantry.add("rice");
        }
        Matcher c = COUNT.matcher(prompt);
        int count = c.find() ? Math.min(Integer.parseInt(c.group(1)), 5) : 3;
        Matcher d = DIET.matcher(prompt);
        String diet = d.find() && d.group(1).equals("VEGAN") ? "VEGAN" : "VEG";

        String a = pantry.get(0);
        String b = pantry.get(pantry.size() > 1 ? 1 : 0);
        String third = pantry.get(pantry.size() > 2 ? 2 : 0);

        List<Map<String, Object>> all = List.of(
                recipe(a + " and " + b + " stir-fry (demo)", 15, "Indian", diet,
                        List.of(ingredient(a, "2", "cups"), ingredient(b, "1", "cup"),
                                ingredient("cooking oil", "1", "tbsp"), ingredient("cumin seeds", "1", "tsp")),
                        List.of("Heat the oil and add the cumin seeds.", "Add " + a + " and " + b + ".",
                                "Stir-fry for 8 minutes.", "Season with salt and serve hot.")),
                recipe(third + " comfort bowl (demo)", 25, "Home style", diet,
                        List.of(ingredient(third, "2", "cups"), ingredient(a, "1", "cup"),
                                ingredient("lemon", "1", "piece"), ingredient("salt", "1", "pinch")),
                        List.of("Cook " + third + " until soft.", "Mix in " + a + ".",
                                "Finish with lemon juice and salt.")),
                recipe("Peanut crunch " + a + " salad (demo)", 10, "Fusion", diet,
                        List.of(ingredient(a, "2", "cups"), ingredient("roasted peanuts", "1/4", "cup"),
                                ingredient("lemon", "1", "piece")),
                        List.of("Chop " + a + ".", "Toss with peanuts and lemon juice.", "Serve fresh.")),
                recipe("One-pot " + b + " and " + third + " (demo)", 30, "Comfort", diet,
                        List.of(ingredient(b, "2", "cups"), ingredient(third, "1", "cup"),
                                ingredient("water", "2", "cups"), ingredient("black pepper", "1", "pinch")),
                        List.of("Add everything to one pot.", "Simmer for 20 minutes.", "Season and serve.")),
                recipe(a + " pan fry (demo)", 12, "Quick", diet,
                        List.of(ingredient(a, "3", "cups"), ingredient("cooking oil", "1", "tbsp"),
                                ingredient("garlic", "3", "cloves")),
                        List.of("Heat the oil.", "Fry the garlic, then " + a + ".", "Cook until golden.")));

        try {
            String json = mapper.writeValueAsString(Map.of("recipes", all.subList(0, Math.min(count, all.size()))));
            return new LlmReply(json, 0);
        } catch (JsonProcessingException e) {
            throw new LlmException("Could not build the demo recipes", e);
        }
    }

    private static Map<String, Object> recipe(String title, int minutes, String cuisine, String diet,
                                              List<Map<String, String>> ingredients, List<String> steps) {
        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("title", title);
        recipe.put("description", "A demo recipe made from your pantry. Add a real API key for real suggestions.");
        recipe.put("cookTimeMinutes", minutes);
        recipe.put("servings", 2);
        recipe.put("cuisine", cuisine);
        recipe.put("dietaryType", diet);
        recipe.put("ingredients", ingredients);
        recipe.put("steps", steps);
        return recipe;
    }

    private static Map<String, String> ingredient(String name, String quantity, String unit) {
        return Map.of("name", name, "quantity", quantity, "unit", unit);
    }
}
