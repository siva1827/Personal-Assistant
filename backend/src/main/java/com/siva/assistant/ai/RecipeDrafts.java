package com.siva.assistant.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** What the model is asked to return. Validated before anything is saved. */
public final class RecipeDrafts {

    private RecipeDrafts() {
    }

    public record IngredientDraft(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 50) String quantity,
            @Size(max = 30) String unit) {
    }

    public record RecipeDraft(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            @NotNull @Min(1) @Max(1440) Integer cookTimeMinutes,
            @NotNull @Min(1) @Max(50) Integer servings,
            @Size(max = 100) String cuisine,
            String dietaryType,
            @NotEmpty @Size(max = 40) List<@Valid IngredientDraft> ingredients,
            @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 500) String> steps) {
    }

    public record RecipeBatch(@NotEmpty @Size(max = 10) List<@Valid RecipeDraft> recipes) {
    }
}
