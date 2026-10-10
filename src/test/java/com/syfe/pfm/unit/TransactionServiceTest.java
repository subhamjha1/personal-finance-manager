package com.syfe.pfm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.syfe.pfm.dto.TransactionDtos.TransactionRequest;
import com.syfe.pfm.dto.TransactionDtos.TransactionResponse;
import com.syfe.pfm.dto.TransactionDtos.TransactionUpdateRequest;
import com.syfe.pfm.entity.Category;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.entity.Transaction;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.CategoryRepository;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.repository.UserRepository;
import com.syfe.pfm.service.TransactionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);
        service = new TransactionService(transactionRepository, categoryRepository, userRepository, clock);
    }

    @Test
    void create_futureDate_throwsBadRequest() {
        TransactionRequest request = new TransactionRequest(new BigDecimal("10"), TODAY.plusDays(1), "Food", null);
        ApiException ex = assertThrows(ApiException.class, () -> service.create(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void create_unknownCategory_throwsBadRequest() {
        when(categoryRepository.findVisibleByName("Nope", 1L)).thenReturn(Optional.empty());
        TransactionRequest request = new TransactionRequest(new BigDecimal("10"), TODAY, "Nope", null);
        ApiException ex = assertThrows(ApiException.class, () -> service.create(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_derivesTypeFromCategoryAndScalesAmount() {
        Category salary = new Category("Salary", CategoryType.INCOME, null);
        when(categoryRepository.findVisibleByName("Salary", 1L)).thenReturn(Optional.of(salary));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TransactionResponse response = service.create(1L,
                new TransactionRequest(new BigDecimal("5"), TODAY, "Salary", "note"));
        assertEquals(CategoryType.INCOME, response.type());
        assertEquals(new BigDecimal("5.00"), response.amount());
    }

    @Test
    void update_differentDate_throwsBadRequest() {
        Transaction tx = new Transaction(null, new Category("Food", CategoryType.EXPENSE, null),
                new BigDecimal("10.00"), TODAY.minusDays(2), null);
        when(transactionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(tx));
        TransactionUpdateRequest request = new TransactionUpdateRequest(null, TODAY.minusDays(1), null, null);
        ApiException ex = assertThrows(ApiException.class, () -> service.update(1L, 5L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void update_missing_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.update(1L, 5L, new TransactionUpdateRequest(null, null, null, "x")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.delete(1L, 5L));
verify(transactionRepository, never()).delete(any(Transaction.class));    }

    @Test
    void list_startAfterEnd_throwsBadRequest() {
        assertThrows(ApiException.class,
                () -> service.list(1L, TODAY, TODAY.minusDays(1), null, null, null));
    }
}
