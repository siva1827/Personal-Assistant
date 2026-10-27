package com.siva.assistant.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares the model's ingredient text with the user's real pantry and allergy list. These checks
 * run in code because the model's own claims about allergies and what the user has cannot be trusted.
 */
public final class IngredientMatcher {

    /** Words that describe how an ingredient is prepared or measured, not what it is. */
    private static final Set<String> DESCRIPTORS = Set.of(
            "fresh", "chopped", "diced", "sliced", "ripe", "large", "small", "medium", "big", "finely",
            "minced", "boiled", "cooked", "grated", "raw", "whole", "dried", "ground", "crushed", "peeled",
            "cubed", "shredded", "roasted", "optional", "handful", "pinch", "some", "of", "a", "the",
            "g", "kg", "ml", "l", "cup", "tbsp", "tsp", "piece", "pc", "pcs");

    private static final List<String> SHELLFISH =
            List.of("prawn", "shrimp", "crab", "lobster", "clam", "oyster", "mussel", "scallop", "crayfish");
    private static final List<String> TREE_NUTS =
            List.of("almond", "cashew", "walnut", "pistachio", "hazelnut", "pecan", "macadamia", "pine nut");
    private static final List<String> DAIRY = List.of("milk", "cheese", "butter", "ghee", "curd", "yogurt",
            "yoghurt", "paneer", "cream", "whey", "khoa", "lassi");
    private static final List<String> GLUTEN = List.of("wheat", "flour", "maida", "atta", "bread", "pasta",
            "semolina", "rava", "suji", "noodle", "couscous", "barley", "rye", "seitan");
    private static final List<String> FISH =
            List.of("fish", "tuna", "salmon", "sardine", "mackerel", "anchovy", "cod", "hilsa");
    private static final List<String> SOY = List.of("soy", "soya", "tofu", "tempeh", "edamame");

    /**
     * An allergy entered as a general group also covers its common members, so "shellfish" catches
     * "prawns" and "dairy" catches "paneer". Keys are in normalized (singular, lower case) form.
     */
    private static final Map<String, List<String>> ALLERGEN_GROUPS = Map.ofEntries(
            Map.entry("shellfish", SHELLFISH),
            Map.entry("seafood", merge(SHELLFISH, FISH)),
            Map.entry("nut", TREE_NUTS),
            Map.entry("tree nut", TREE_NUTS),
            Map.entry("peanut", List.of("groundnut")),
            Map.entry("dairy", DAIRY),
            Map.entry("milk", DAIRY),
            Map.entry("lactose", DAIRY),
            Map.entry("gluten", GLUTEN),
            Map.entry("wheat", GLUTEN),
            Map.entry("fish", FISH),
            Map.entry("soy", SOY),
            Map.entry("soya", SOY),
            Map.entry("egg", List.of("mayonnaise")),
            Map.entry("sesame", List.of("tahini", "til")));

    private IngredientMatcher() {
    }

    /**
     * True when the recipe ingredient is something in the pantry, ignoring plurals, quantities
     * and preparation words. "2 ripe tomatoes" matches the pantry item "tomato", but "sweet
     * potato" does not match a pantry item that is only "potato".
     */
    public static boolean inPantry(String recipeIngredient, Collection<String> pantryNames) {
        Set<String> recipeWords = words(recipeIngredient);
        if (recipeWords.isEmpty()) {
            return false;
        }
        for (String pantry : pantryNames) {
            Set<String> pantryWords = words(pantry);
            if (pantryWords.isEmpty() || !recipeWords.containsAll(pantryWords)) {
                continue;
            }
            Set<String> extra = new HashSet<>(recipeWords);
            extra.removeAll(pantryWords);
            if (DESCRIPTORS.containsAll(extra)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the text mentions an allergen or a common member of an allergen group. This is
     * deliberately cautious: it looks for the allergen anywhere inside the text, so "nut" also
     * catches "peanut" and "coconut", and "dairy" catches "peanut butter". A recipe dropped by
     * mistake costs a retry; a missed allergen costs far more.
     */
    public static boolean mentionsAllergen(String text, Collection<String> allergens) {
        String haystack = normalized(text);
        for (String needle : expand(allergens)) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /** The user's allergies plus the members of any general groups among them, normalized. */
    static Set<String> expand(Collection<String> allergens) {
        Set<String> needles = new HashSet<>();
        for (String allergen : allergens) {
            String normal = normalized(allergen);
            if (normal.isEmpty()) {
                continue;
            }
            needles.add(normal);
            for (String member : ALLERGEN_GROUPS.getOrDefault(normal, List.of())) {
                needles.add(normalized(member));
            }
        }
        return needles;
    }

    static Set<String> words(String text) {
        return Arrays.stream(normalized(text).split(" "))
                .filter(w -> !w.isEmpty() && !w.chars().allMatch(Character::isDigit))
                .collect(Collectors.toSet());
    }

    /** Lower case, letters and digits only, each word reduced to a plural-free, spelling-neutral form. */
    static String normalized(String text) {
        if (text == null) {
            return "";
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{M}\\p{N}]+", " ").trim().split(" "))
                .filter(w -> !w.isEmpty())
                .map(IngredientMatcher::stem)
                .collect(Collectors.joining(" "));
    }

    /** Singular form, with a trailing i written as y so "chilli" and "chillies" end up identical. */
    static String stem(String word) {
        String singular = singular(word);
        if (singular.length() > 3 && singular.endsWith("i")) {
            return singular.substring(0, singular.length() - 1) + "y";
        }
        return singular;
    }

    static String singular(String word) {
        if (word.length() <= 3) {
            return word;
        }
        if (word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("oes") || word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private static List<String> merge(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
