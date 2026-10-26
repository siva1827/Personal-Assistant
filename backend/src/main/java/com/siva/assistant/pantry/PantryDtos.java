package com.siva.assistant.pantry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PantryDtos {

    private PantryDtos() {
    }

    public record AddItemRequest(
            @NotNull Long ingredientId,
            @DecimalMin("0.0") BigDecimal quantity,
            @Size(max = 20) String unit,
            LocalDate expiryDate) {
    }

    public record BulkAddRequest(@NotEmpty @Size(max = 100) List<@NotNull Long> ingredientIds) {
    }

    /** Fields left null are not changed. */
    public record UpdateItemRequest(
            @DecimalMin("0.0") BigDecimal quantity,
            @Size(max = 20) String unit,
            LocalDate expiryDate) {
    }

    public record ItemResponse(
            Long id,
            Long ingredientId,
            String ingredientName,
            String category,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate,
            Instant addedAt) {

        static ItemResponse from(PantryItem item) {
            return new ItemResponse(item.getId(), item.getIngredient().getId(), item.getIngredient().getName(),
                    item.getIngredient().getCategory(), item.getQuantity(), item.getUnit(), item.getExpiryDate(),
                    item.getAddedAt());
        }
    }
}
