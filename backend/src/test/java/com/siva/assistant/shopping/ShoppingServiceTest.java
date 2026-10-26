package com.siva.assistant.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.recipe.Recipe;
import com.siva.assistant.recipe.RecipeRepository;
import com.siva.assistant.shopping.ShoppingDtos.AddItemRequest;
import com.siva.assistant.shopping.ShoppingDtos.UpdateItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShoppingServiceTest {

    private static final long USER = 7L;

    @Mock
    ShoppingItemRepository items;
    @Mock
    RecipeRepository recipes;

    ShoppingService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingService(items, recipes, new ObjectMapper());
    }

    private static Recipe recipeMissing(String json) throws Exception {
        var ctor = Recipe.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Recipe r = ctor.newInstance();
        ReflectionTestUtils.setField(r, "missingIngredients", json);
        return r;
    }

    @Test
    void addRejectsADuplicateStillToBuy() {
        when(items.findByUserIdAndPurchasedFalse(USER))
                .thenReturn(List.of(new ShoppingItem(USER, "Tomato", null, null)));

        assertThatThrownBy(() -> service.add(USER, new AddItemRequest(" tomato ", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(items, never()).save(any());
    }

    @Test
    void addAllowsAnItemThatWasAlreadyBought() {
        when(items.findByUserIdAndPurchasedFalse(USER)).thenReturn(List.of());
        when(items.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var added = service.add(USER, new AddItemRequest("  Milk ", new BigDecimal("60")));

        assertThat(added.name()).isEqualTo("Milk");
        assertThat(added.purchased()).isFalse();
        assertThat(added.estimatedCost()).isEqualByComparingTo("60");
    }

    @Test
    void fromRecipeAddsOnlyWhatIsNotAlreadyWaiting() throws Exception {
        when(recipes.findByIdAndUserId(3L, USER))
                .thenReturn(Optional.of(recipeMissing("[\"cumin seeds\",\"Tomato\",\"gram flour\",\"cumin seeds\"]")));
        when(items.findByUserIdAndPurchasedFalse(USER))
                .thenReturn(List.of(new ShoppingItem(USER, "tomato", null, null)));
        when(items.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addFromRecipe(USER, 3L);

        assertThat(result.added()).extracting("name").containsExactly("cumin seeds", "gram flour");
        assertThat(result.added()).allMatch(i -> i.sourceRecipeId() == 3L);
        // "Tomato" is on the list already and the second "cumin seeds" repeats the first
        assertThat(result.skipped()).isEqualTo(2);
    }

    @Test
    void fromRecipeOfAnotherUserIsNotFound() {
        when(recipes.findByIdAndUserId(3L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFromRecipe(USER, 3L)).isInstanceOf(ApiException.class);
    }

    @Test
    void updateCanMarkBoughtAndClearTheCost() {
        ShoppingItem item = new ShoppingItem(USER, "Rice", new BigDecimal("90"), null);
        when(items.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(item));

        service.update(USER, 5L, new UpdateItemRequest(null, null, true, true));

        assertThat(item.isPurchased()).isTrue();
        assertThat(item.getEstimatedCost()).isNull();
        assertThat(item.getName()).isEqualTo("Rice");
    }

    @Test
    void clearPurchasedReturnsHowManyWereRemoved() {
        when(items.deleteByUserIdAndPurchasedTrue(USER)).thenReturn(3L);

        assertThat(service.clearPurchased(USER)).isEqualTo(3L);
    }
}
