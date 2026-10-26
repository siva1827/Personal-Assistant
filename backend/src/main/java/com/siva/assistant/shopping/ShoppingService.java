package com.siva.assistant.shopping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.recipe.Recipe;
import com.siva.assistant.recipe.RecipeRepository;
import com.siva.assistant.shopping.ShoppingDtos.AddItemRequest;
import com.siva.assistant.shopping.ShoppingDtos.AddedFromRecipe;
import com.siva.assistant.shopping.ShoppingDtos.ItemResponse;
import com.siva.assistant.shopping.ShoppingDtos.UpdateItemRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingService {

    private static final TypeReference<List<String>> NAMES = new TypeReference<>() {
    };

    private final ShoppingItemRepository items;
    private final RecipeRepository recipes;
    private final ObjectMapper objectMapper;

    public ShoppingService(ShoppingItemRepository items, RecipeRepository recipes, ObjectMapper objectMapper) {
        this.items = items;
        this.recipes = recipes;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(long userId) {
        return items.findByUserIdOrderByPurchasedAscIdAsc(userId).stream().map(ItemResponse::from).toList();
    }

    @Transactional
    public ItemResponse add(long userId, AddItemRequest request) {
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        if (toBuyNames(userId).contains(key(name))) {
            throw new ApiException(HttpStatus.CONFLICT, name + " is already on your list");
        }
        return ItemResponse.from(items.save(new ShoppingItem(userId, name, request.estimatedCost(), null)));
    }

    /** Adds the recipe's missing ingredients that are not already waiting on the list. */
    @Transactional
    public AddedFromRecipe addFromRecipe(long userId, long recipeId) {
        Recipe recipe = recipes.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipe not found"));
        Set<String> waiting = toBuyNames(userId);
        List<ShoppingItem> created = new ArrayList<>();
        int skipped = 0;
        for (String raw : missingIngredients(recipe)) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (!waiting.add(key(name))) {
                skipped++;
                continue;
            }
            created.add(new ShoppingItem(userId, name, null, recipeId));
        }
        List<ItemResponse> added = items.saveAll(created).stream().map(ItemResponse::from).toList();
        return new AddedFromRecipe(added, skipped);
    }

    @Transactional
    public ItemResponse update(long userId, long itemId, UpdateItemRequest request) {
        ShoppingItem item = find(userId, itemId);
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "name must not be blank");
            }
            item.setName(name);
        }
        if (Boolean.TRUE.equals(request.clearCost())) {
            item.setEstimatedCost(null);
        } else if (request.estimatedCost() != null) {
            item.setEstimatedCost(request.estimatedCost());
        }
        if (request.purchased() != null) {
            item.setPurchased(request.purchased());
        }
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(long userId, long itemId) {
        items.delete(find(userId, itemId));
    }

    /** Removes everything already bought; returns how many were removed. */
    @Transactional
    public long clearPurchased(long userId) {
        return items.deleteByUserIdAndPurchasedTrue(userId);
    }

    private ShoppingItem find(long userId, long itemId) {
        return items.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private Set<String> toBuyNames(long userId) {
        return items.findByUserIdAndPurchasedFalse(userId).stream()
                .map(i -> key(i.getName()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> missingIngredients(Recipe recipe) {
        try {
            return objectMapper.readValue(recipe.getMissingIngredients(), NAMES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored recipe JSON is invalid", e);
        }
    }
}
