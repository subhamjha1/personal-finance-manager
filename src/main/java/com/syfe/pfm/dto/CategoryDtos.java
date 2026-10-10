package com.syfe.pfm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syfe.pfm.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request/response bodies of the category endpoints. */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "name is required")
            @Size(max = 50, message = "name must be at most 50 characters")
            String name,

            @NotNull(message = "type is required (INCOME or EXPENSE)")
            CategoryType type) {
    }

    public record CategoryResponse(String name, CategoryType type, @JsonProperty("isCustom") boolean custom) {
    }

    public record CategoryListResponse(List<CategoryResponse> categories) {
    }
}
