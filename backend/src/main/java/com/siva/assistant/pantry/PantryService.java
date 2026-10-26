package com.siva.assistant.pantry;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.ingredient.Ingredient;
import com.siva.assistant.ingredient.IngredientRepository;
import com.siva.assistant.pantry.PantryDtos.AddItemRequest;
import com.siva.assistant.pantry.PantryDtos.BulkAddRequest;
import com.siva.assistant.pantry.PantryDtos.ItemResponse;
import com.siva.assistant.pantry.PantryDtos.UpdateItemRequest;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryService {

    private final PantryItemRepository pantry;
    private final IngredientRepository ingredients;

    public PantryService(PantryItemRepository pantry, IngredientRepository ingredients) {
        this.pantry = pantry;
        this.ingredients = ingredients;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(long userId) {
        return pantry.findByUserIdOrderByAddedAtDescIdDesc(userId).stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> expiring(long userId, int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return pantry.findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDate(userId, cutoff).stream()
                .map(ItemResponse::from).toList();
    }

    @Transactional
    public ItemResponse add(long userId, AddItemRequest request) {
        Ingredient ingredient = ingredients.findById(request.ingredientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ingredient not found"));
        if (pantry.existsByUserIdAndIngredientId(userId, ingredient.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ingredient.getName() + " is already in your pantry");
        }
        PantryItem item = new PantryItem(userId, ingredient);
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setExpiryDate(request.expiryDate());
        return ItemResponse.from(pantry.save(item));
    }

    /** Adds every known ingredient not already in the pantry; unknown ids are rejected, duplicates skipped. */
    @Transactional
    public List<ItemResponse> addBulk(long userId, BulkAddRequest request) {
        Set<Long> requested = new LinkedHashSet<>(request.ingredientIds());
        List<Ingredient> found = ingredients.findAllById(requested);
        if (found.size() != requested.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "One or more ingredients not found");
        }
        Set<Long> existing = pantry.findExistingIngredientIds(userId, requested);
        List<PantryItem> created = found.stream()
                .filter(i -> !existing.contains(i.getId()))
                .map(i -> new PantryItem(userId, i))
                .toList();
        return pantry.saveAll(created).stream().map(ItemResponse::from).toList();
    }

    @Transactional
    public ItemResponse update(long userId, long itemId, UpdateItemRequest request) {
        PantryItem item = find(userId, itemId);
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.unit() != null) {
            item.setUnit(request.unit());
        }
        if (request.expiryDate() != null) {
            item.setExpiryDate(request.expiryDate());
        }
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(long userId, long itemId) {
        pantry.delete(find(userId, itemId));
    }

    private PantryItem find(long userId, long itemId) {
        // Scoped to the user so other users' items look like they do not exist
        return pantry.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pantry item not found"));
    }
}
