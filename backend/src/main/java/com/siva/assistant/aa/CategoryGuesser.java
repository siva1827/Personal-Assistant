package com.siva.assistant.aa;

import com.siva.assistant.category.Category;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

/** Picks one of the user's own categories for an imported transaction from keywords in its text. */
public final class CategoryGuesser {

    /** Same keywords as the app's SMS import, so both routes categorise alike. */
    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("Food", List.of("swiggy", "zomato", "restaurant", "cafe", "dominos", "mcdonald", "kfc",
                "pizza", "starbucks"));
        KEYWORDS.put("Groceries", List.of("bigbasket", "blinkit", "zepto", "dmart", "grocery", "supermarket",
                "instamart", "kirana", "reliance fresh"));
        KEYWORDS.put("Transport", List.of("uber", "ola cab", "rapido", "irctc", "metro", "redbus", "fuel", "petrol",
                "indian oil", "hpcl", "bpcl"));
        KEYWORDS.put("Shopping", List.of("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa"));
        KEYWORDS.put("Bills", List.of("electricity", "bescom", "airtel", "jio", "vodafone", "bsnl", "broadband",
                "recharge", "insurance", "gas"));
        KEYWORDS.put("Health", List.of("pharmacy", "apollo", "medplus", "hospital", "clinic", "pharmeasy", "1mg"));
        KEYWORDS.put("Entertainment", List.of("netflix", "spotify", "bookmyshow", "hotstar", "prime video", "pvr",
                "inox"));
    }

    private CategoryGuesser() {
    }

    /**
     * The matching category from the user's list, else "Other", else the first one. Returns null only
     * when the user has no categories at all.
     */
    public static Category choose(String text, List<Category> categories) {
        String haystack = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(haystack::contains)) {
                Category match = byName(entry.getKey(), categories);
                if (match != null) {
                    return match;
                }
            }
        }
        Category other = byName("Other", categories);
        return other != null ? other : categories.isEmpty() ? null : categories.get(0);
    }

    private static Category byName(String name, List<Category> categories) {
        return categories.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
