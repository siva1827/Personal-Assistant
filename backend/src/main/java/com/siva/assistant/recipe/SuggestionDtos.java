package com.siva.assistant.recipe;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class SuggestionDtos {

    private SuggestionDtos() {
    }

    /**
     * themeId and customNote are both optional. count defaults to 3. fresh skips the short-term
     * cache and asks the model again (it still counts towards the daily limit).
     */
    public record SuggestRequest(
            Long themeId,
            @Size(max = 200) String customNote,
            @Min(1) @Max(5) Integer count,
            Boolean fresh) {
    }

    /**
     * cached is true when recent suggestions for the same pantry were reused. filteredOut counts
     * recipes removed because they clashed with the user's allergies or diet.
     */
    public record SuggestionResponse(List<RecipeResponse> recipes, boolean cached, int filteredOut, String model) {
    }
}
