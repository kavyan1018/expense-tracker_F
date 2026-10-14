package com.expensetracker.dto;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.TxnType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "Name is required") @Size(max = 60) String name,
            @NotNull(message = "Type is required") TxnType type,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must look like #1a2b3c") String color) {
    }

    public record CategoryResponse(Long id, String name, TxnType type, String color, boolean archived) {
        public static CategoryResponse of(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getColor(), c.isArchived());
        }
    }

    /** What happened on delete: removed outright, or archived because records still use it. */
    public record DeleteResult(boolean deleted, boolean archived) {
    }
}
