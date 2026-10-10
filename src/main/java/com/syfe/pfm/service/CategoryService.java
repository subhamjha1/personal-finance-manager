package com.syfe.pfm.service;

import com.syfe.pfm.dto.CategoryDtos.CategoryListResponse;
import com.syfe.pfm.dto.CategoryDtos.CategoryRequest;
import com.syfe.pfm.dto.CategoryDtos.CategoryResponse;
import com.syfe.pfm.entity.Category;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.CategoryRepository;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category rules. Names are unique case-insensitively among the categories visible to a user
 * (defaults + own custom ones). Default categories are immutable.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                           UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /** Lists default categories followed by the user's custom categories. */
    @Transactional(readOnly = true)
    public CategoryListResponse list(Long userId) {
        return new CategoryListResponse(categoryRepository.findVisibleToUser(userId).stream()
                .map(CategoryService::toResponse).toList());
    }

    /** Creates a custom category; 409 if the name is already visible to the user. */
    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw ApiException.badRequest("name must not be blank");
        }
        if (categoryRepository.findVisibleByName(name, userId).isPresent()) {
            throw ApiException.conflict("Category '" + name + "' already exists");
        }
        Category saved = categoryRepository.save(
                new Category(name, request.type(), userRepository.getReferenceById(userId)));
        return toResponse(saved);
    }

    /**
     * Deletes a custom category owned by the user.
     *
     * @throws ApiException 404 if not visible to the user, 403 for a default category, 400 if still referenced
     */
    @Transactional
    public void delete(Long userId, String name) {
        Category category = categoryRepository.findVisibleByName(name.trim(), userId)
                .orElseThrow(() -> ApiException.notFound("Category '" + name + "' not found"));
        if (!category.isCustom()) {
            throw ApiException.forbidden("Default categories cannot be deleted");
        }
        if (transactionRepository.existsByCategoryId(category.getId())) {
            throw ApiException.badRequest("Category '" + category.getName() + "' is used by existing transactions");
        }
        categoryRepository.delete(category);
    }

    static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getName(), c.getType(), c.isCustom());
    }
}
