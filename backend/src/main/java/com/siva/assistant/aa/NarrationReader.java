package com.siva.assistant.aa;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Bank statements describe a transaction in one cryptic line such as
 * "UPI/DR/412345678/SWIGGY/okhdfcbank". This pulls out a readable name and a category hint.
 */
public final class NarrationReader {

    /** Parts of a narration that describe how the money moved, not who it went to. */
    private static final Set<String> NOISE = Set.of("UPI", "DR", "CR", "POS", "ATM", "WDL", "NEFT", "IMPS", "ACH",
            "RTGS", "ECS", "NACH", "TXN");

    private static final List<String> HANDLE_SUFFIXES = List.of("okhdfcbank", "okicici", "oksbi", "okaxis", "ybl",
            "paytm", "upi", "ibl", "axl");

    private NarrationReader() {
    }

    /** The first part that is a name: not a number, not a payment-mode word, not a UPI handle. */
    public static Optional<String> merchant(String narration) {
        if (narration == null) {
            return Optional.empty();
        }
        for (String part : narration.split("/")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty() || trimmed.chars().allMatch(Character::isDigit)
                    || NOISE.contains(trimmed.toUpperCase(Locale.ROOT)) || isHandle(trimmed)) {
                continue;
            }
            return Optional.of(trimmed);
        }
        return Optional.empty();
    }

    /** A short note for the expense: the merchant, or a plain description for cash withdrawals. */
    public static String note(String narration) {
        if (narration != null && narration.toUpperCase(Locale.ROOT).startsWith("ATM/")) {
            return "ATM withdrawal";
        }
        String note = merchant(narration).orElse(narration == null ? "Bank transaction" : narration);
        return note.length() <= 200 ? note : note.substring(0, 200);
    }

    private static boolean isHandle(String part) {
        String lower = part.toLowerCase(Locale.ROOT);
        return lower.contains("@") || HANDLE_SUFFIXES.contains(lower);
    }
}
