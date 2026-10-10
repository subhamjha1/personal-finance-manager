package com.syfe.pfm.controller;

import com.syfe.pfm.dto.CategoryDtos.CategoryListResponse;
import com.syfe.pfm.dto.CategoryDtos.CategoryRequest;
import com.syfe.pfm.dto.CategoryDtos.CategoryResponse;
import com.syfe.pfm.dto.MessageResponse;
import com.syfe.pfm.security.UserPrincipal;
import com.syfe.pfm.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Category endpoints. The user is always taken from the authenticated principal. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public CategoryListResponse list(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.list(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(principal.getId(), request);
    }

    @DeleteMapping("/{name}")
    public MessageResponse delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String name) {
        categoryService.delete(principal.getId(), name);
        return new MessageResponse("Category deleted successfully");
    }
}
