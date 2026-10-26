package com.siva.assistant.shopping;

import com.siva.assistant.shopping.ShoppingDtos.AddItemRequest;
import com.siva.assistant.shopping.ShoppingDtos.AddedFromRecipe;
import com.siva.assistant.shopping.ShoppingDtos.ItemResponse;
import com.siva.assistant.shopping.ShoppingDtos.UpdateItemRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopping")
public class ShoppingController {

    private final ShoppingService shoppingService;

    public ShoppingController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @GetMapping
    public List<ItemResponse> list(@AuthenticationPrincipal Long userId) {
        return shoppingService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse add(@AuthenticationPrincipal Long userId, @Valid @RequestBody AddItemRequest request) {
        return shoppingService.add(userId, request);
    }

    @PostMapping("/from-recipe/{recipeId}")
    public AddedFromRecipe addFromRecipe(@AuthenticationPrincipal Long userId, @PathVariable Long recipeId) {
        return shoppingService.addFromRecipe(userId, recipeId);
    }

    @PatchMapping("/{id}")
    public ItemResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                               @Valid @RequestBody UpdateItemRequest request) {
        return shoppingService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        shoppingService.delete(userId, id);
    }

    /** Clears everything already marked as bought. */
    @DeleteMapping("/purchased")
    public Map<String, Long> clearPurchased(@AuthenticationPrincipal Long userId) {
        return Map.of("removed", shoppingService.clearPurchased(userId));
    }
}
