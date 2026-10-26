package com.siva.assistant.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.favorite.FavoriteId;
import com.siva.assistant.favorite.FavoriteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final long USER = 7L;

    @Mock
    RecipeRepository recipes;
    @Mock
    FavoriteRepository favorites;

    RecipeService service;

    @BeforeEach
    void setUp() {
        service = new RecipeService(recipes, favorites, new ObjectMapper());
    }

    private static Recipe newRecipe(long id) throws Exception {
        var ctor = Recipe.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Recipe r = ctor.newInstance();
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "title", "Dal");
        ReflectionTestUtils.setField(r, "ingredients", "[{\"name\":\"toor dal\",\"in_pantry\":true}]");
        ReflectionTestUtils.setField(r, "missingIngredients", "[\"cumin\"]");
        ReflectionTestUtils.setField(r, "steps", "[\"Boil\",\"Temper\"]");
        return r;
    }

    @Test
    void getParsesStoredJsonAndFlagsFavorite() throws Exception {
        when(recipes.findByIdAndUserId(3L, USER)).thenReturn(Optional.of(newRecipe(3L)));
        when(favorites.existsById(new FavoriteId(USER, 3L))).thenReturn(true);

        RecipeResponse response = service.get(USER, 3L);

        assertThat(response.favorite()).isTrue();
        assertThat(response.missingIngredients().get(0).asText()).isEqualTo("cumin");
        assertThat(response.steps()).hasSize(2);
        assertThat(response.ingredients().get(0).get("name").asText()).isEqualTo("toor dal");
    }

    @Test
    void otherUsersRecipeIsNotFound() {
        when(recipes.findByIdAndUserId(3L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(USER, 3L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.addFavorite(USER, 3L)).isInstanceOf(ApiException.class);
        verify(favorites, never()).save(any());
    }

    @Test
    void addFavoriteIsIdempotent() throws Exception {
        when(recipes.findByIdAndUserId(3L, USER)).thenReturn(Optional.of(newRecipe(3L)));
        when(favorites.existsById(new FavoriteId(USER, 3L))).thenReturn(true);

        service.addFavorite(USER, 3L);

        verify(favorites, never()).save(any());
    }

    @Test
    void removeFavoriteIgnoresRecipesThatAreNotFavorites() {
        when(favorites.existsById(new FavoriteId(USER, 3L))).thenReturn(false);

        service.removeFavorite(USER, 3L);

        verify(favorites, never()).deleteById(any());
    }
}
