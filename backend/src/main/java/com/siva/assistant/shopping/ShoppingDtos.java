package com.siva.assistant.shopping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class ShoppingDtos {

    private ShoppingDtos() {
    }

    public record AddItemRequest(
            @NotBlank @Size(max = 100) String name,
            @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal estimatedCost) {
    }

    /** Fields left null are not changed; set clearCost to remove the estimate. */
    public record UpdateItemRequest(
            @Size(min = 1, max = 100) String name,
            @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal estimatedCost,
            Boolean clearCost,
            Boolean purchased) {
    }

    public record ItemResponse(Long id, String name, BigDecimal estimatedCost, boolean purchased,
                               Long sourceRecipeId) {

        static ItemResponse from(ShoppingItem item) {
            return new ItemResponse(item.getId(), item.getName(), item.getEstimatedCost(), item.isPurchased(),
                    item.getSourceRecipeId());
        }
    }

    /** skipped counts missing ingredients that were already on the list. */
    public record AddedFromRecipe(List<ItemResponse> added, int skipped) {
    }
}
