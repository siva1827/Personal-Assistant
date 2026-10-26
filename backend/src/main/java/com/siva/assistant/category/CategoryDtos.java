package com.siva.assistant.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CreateCategoryRequest(@NotBlank @Size(max = 50) String name, @Size(max = 16) String emoji) {
    }

    public record CategoryResponse(Long id, String name, String emoji) {

        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.getEmoji());
        }
    }
}
