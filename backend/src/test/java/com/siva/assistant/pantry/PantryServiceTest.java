package com.siva.assistant.pantry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.ingredient.Ingredient;
import com.siva.assistant.ingredient.IngredientRepository;
import com.siva.assistant.pantry.PantryDtos.AddItemRequest;
import com.siva.assistant.pantry.PantryDtos.BulkAddRequest;
import com.siva.assistant.pantry.PantryDtos.UpdateItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PantryServiceTest {

    private static final long USER = 7L;

    @Mock
    PantryItemRepository pantry;
    @Mock
    IngredientRepository ingredients;

    PantryService service;

    @BeforeEach
    void setUp() {
        service = new PantryService(pantry, ingredients);
    }

    private static Ingredient ingredient(long id, String name) {
        Ingredient i = new Ingredient();
        ReflectionTestUtils.setField(i, "id", id);
        ReflectionTestUtils.setField(i, "name", name);
        return i;
    }

    @Test
    void addRejectsUnknownIngredient() {
        when(ingredients.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(USER, new AddItemRequest(1L, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addRejectsDuplicate() {
        when(ingredients.findById(1L)).thenReturn(Optional.of(ingredient(1L, "onion")));
        when(pantry.existsByUserIdAndIngredientId(USER, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.add(USER, new AddItemRequest(1L, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(pantry, never()).save(any());
    }

    @Test
    void bulkAddSkipsIngredientsAlreadyInPantry() {
        Ingredient onion = ingredient(1L, "onion");
        Ingredient tomato = ingredient(2L, "tomato");
        when(ingredients.findAllById(Set.of(1L, 2L))).thenReturn(List.of(onion, tomato));
        when(pantry.findExistingIngredientIds(USER, Set.of(1L, 2L))).thenReturn(Set.of(1L));
        when(pantry.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addBulk(USER, new BulkAddRequest(List.of(1L, 2L, 2L)));

        assertThat(result).extracting("ingredientName").containsExactly("tomato");
    }

    @Test
    void bulkAddRejectsUnknownIngredient() {
        when(ingredients.findAllById(Set.of(1L, 99L))).thenReturn(List.of(ingredient(1L, "onion")));

        assertThatThrownBy(() -> service.addBulk(USER, new BulkAddRequest(List.of(1L, 99L))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        PantryItem item = new PantryItem(USER, ingredient(1L, "onion"));
        item.setQuantity(BigDecimal.ONE);
        item.setUnit("kg");
        when(pantry.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(item));

        service.update(USER, 5L, new UpdateItemRequest(BigDecimal.TEN, null, null));

        assertThat(item.getQuantity()).isEqualByComparingTo("10");
        assertThat(item.getUnit()).isEqualTo("kg");
    }

    @Test
    void otherUsersItemIsNotFound() {
        when(pantry.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER, 5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
