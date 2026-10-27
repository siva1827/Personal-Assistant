package com.siva.assistant.recipe;

import com.siva.assistant.common.PageResponse;
import com.siva.assistant.recipe.SuggestionDtos.SuggestRequest;
import com.siva.assistant.recipe.SuggestionDtos.SuggestionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes")
@Validated
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeSuggestionService suggestionService;

    public RecipeController(RecipeService recipeService, RecipeSuggestionService suggestionService) {
        this.recipeService = recipeService;
        this.suggestionService = suggestionService;
    }

    /** Asks the AI for recipes from the pantry. 429 over the daily limit, 502 if the AI keeps failing, 503 if not set up. */
    @PostMapping("/suggest")
    public SuggestionResponse suggest(@AuthenticationPrincipal Long userId, @Valid @RequestBody SuggestRequest request) {
        return suggestionService.suggest(userId, request);
    }

    @GetMapping("/history")
    public PageResponse<RecipeResponse> history(@AuthenticationPrincipal Long userId,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return recipeService.history(userId, page, size);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return recipeService.get(userId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        recipeService.delete(userId, id);
    }
}
