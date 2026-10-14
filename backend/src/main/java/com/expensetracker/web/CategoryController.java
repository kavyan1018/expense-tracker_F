package com.expensetracker.web;

import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.CategoryDtos.CategoryRequest;
import com.expensetracker.dto.CategoryDtos.CategoryResponse;
import com.expensetracker.dto.CategoryDtos.DeleteResult;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) TxnType type,
                                       @RequestParam(defaultValue = "false") boolean includeArchived) {
        return categories.list(CurrentUser.id(), type, includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest req) {
        return categories.create(CurrentUser.id(), req);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return categories.update(CurrentUser.id(), id, req);
    }

    @DeleteMapping("/{id}")
    public DeleteResult delete(@PathVariable Long id) {
        return categories.delete(CurrentUser.id(), id);
    }
}
