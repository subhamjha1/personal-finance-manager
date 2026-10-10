package com.syfe.pfm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.syfe.pfm.dto.CategoryDtos.CategoryRequest;
import com.syfe.pfm.entity.Category;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.CategoryRepository;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.repository.UserRepository;
import com.syfe.pfm.service.CategoryService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_duplicate_throwsConflict() {
        when(categoryRepository.findVisibleByName("Food", 1L))
                .thenReturn(Optional.of(new Category("Food", CategoryType.EXPENSE, null)));
        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.create(1L, new CategoryRequest("Food", CategoryType.EXPENSE)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_unknown_throwsNotFound() {
        when(categoryRepository.findVisibleByName("X", 1L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> categoryService.delete(1L, "X"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void delete_default_throwsForbidden() {
        Category defaultCategory = new Category("Salary", CategoryType.INCOME, null);
        when(categoryRepository.findVisibleByName("Salary", 1L)).thenReturn(Optional.of(defaultCategory));
        ApiException ex = assertThrows(ApiException.class, () -> categoryService.delete(1L, "Salary"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_referenced_throwsBadRequest() {
        Category custom = mock(Category.class);
        when(custom.isCustom()).thenReturn(true);
        when(custom.getId()).thenReturn(5L);
        when(custom.getName()).thenReturn("Mine");
        when(categoryRepository.findVisibleByName("Mine", 1L)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class, () -> categoryService.delete(1L, "Mine"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_unreferencedCustom_deletesIt() {
        Category custom = mock(Category.class);
        when(custom.isCustom()).thenReturn(true);
        when(custom.getId()).thenReturn(5L);
        when(categoryRepository.findVisibleByName("Mine", 1L)).thenReturn(Optional.of(custom));
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(false);
        categoryService.delete(1L, "Mine");
        verify(categoryRepository).delete(custom);
    }
}
