package com.siva.assistant.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.common.PageResponse;
import com.siva.assistant.favorite.FavoriteId;
import com.siva.assistant.favorite.FavoriteRecipe;
import com.siva.assistant.favorite.FavoriteRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final FavoriteRepository favorites;
    private final ObjectMapper objectMapper;

    public RecipeService(RecipeRepository recipes, FavoriteRepository favorites, ObjectMapper objectMapper) {
        this.recipes = recipes;
        this.favorites = favorites;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RecipeResponse get(long userId, long recipeId) {
        Recipe recipe = find(userId, recipeId);
        boolean favorite = favorites.existsById(new FavoriteId(userId, recipeId));
        return toResponse(recipe, favorite);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> history(long userId, int page, int size) {
        return toPage(userId, recipes.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> favorites(long userId, int page, int size) {
        return toPage(userId, recipes.findFavorites(userId, PageRequest.of(page, size)));
    }

    @Transactional
    public void delete(long userId, long recipeId) {
        recipes.delete(find(userId, recipeId));
    }

    /** Idempotent: saving an already-saved recipe does nothing. */
    @Transactional
    public void addFavorite(long userId, long recipeId) {
        find(userId, recipeId);
        FavoriteId id = new FavoriteId(userId, recipeId);
        if (!favorites.existsById(id)) {
            favorites.save(new FavoriteRecipe(id));
        }
    }

    /** Idempotent: removing a recipe that is not a favorite does nothing. */
    @Transactional
    public void removeFavorite(long userId, long recipeId) {
        FavoriteId id = new FavoriteId(userId, recipeId);
        if (favorites.existsById(id)) {
            favorites.deleteById(id);
        }
    }

    private PageResponse<RecipeResponse> toPage(long userId, Page<Recipe> page) {
        List<Long> ids = page.getContent().stream().map(Recipe::getId).toList();
        Set<Long> favoriteIds = ids.isEmpty() ? Set.of() : favorites.findFavoriteIds(userId, ids);
        return PageResponse.of(page, r -> toResponse(r, favoriteIds.contains(r.getId())));
    }

    private Recipe find(long userId, long recipeId) {
        // Scoped to the user so other users' recipes look like they do not exist
        return recipes.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipe not found"));
    }

    private RecipeResponse toResponse(Recipe r, boolean favorite) {
        return new RecipeResponse(r.getId(), r.getTitle(), r.getDescription(), r.getCookTimeMinutes(),
                r.getServings(), r.getCuisine(), r.getDietaryType(), parse(r.getIngredients()),
                parse(r.getMissingIngredients()), parse(r.getSteps()), favorite, r.getCreatedAt());
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored recipe JSON is invalid", e);
        }
    }
}
