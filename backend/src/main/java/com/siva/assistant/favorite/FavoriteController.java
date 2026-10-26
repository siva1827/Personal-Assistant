package com.siva.assistant.favorite;

import com.siva.assistant.common.PageResponse;
import com.siva.assistant.recipe.RecipeResponse;
import com.siva.assistant.recipe.RecipeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@Validated
public class FavoriteController {

    private final RecipeService recipeService;

    public FavoriteController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public PageResponse<RecipeResponse> list(@AuthenticationPrincipal Long userId,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return recipeService.favorites(userId, page, size);
    }

    @PutMapping("/{recipeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@AuthenticationPrincipal Long userId, @PathVariable Long recipeId) {
        recipeService.addFavorite(userId, recipeId);
    }

    @DeleteMapping("/{recipeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Long userId, @PathVariable Long recipeId) {
        recipeService.removeFavorite(userId, recipeId);
    }
}
