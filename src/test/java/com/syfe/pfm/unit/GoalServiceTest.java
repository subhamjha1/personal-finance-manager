package com.syfe.pfm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.syfe.pfm.dto.GoalDtos.GoalRequest;
import com.syfe.pfm.dto.GoalDtos.GoalResponse;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.entity.SavingsGoal;
import com.syfe.pfm.entity.User;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.SavingsGoalRepository;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.repository.UserRepository;
import com.syfe.pfm.service.GoalService;
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
class GoalServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    @Mock
    private SavingsGoalRepository goalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    private GoalService goalService;
    private User owner;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);
        goalService = new GoalService(goalRepository, transactionRepository, userRepository, clock);
        owner = mock(User.class);
    }

    private SavingsGoal goalOwnedBy(long ownerId) {
        when(owner.getId()).thenReturn(ownerId);
        return new SavingsGoal(owner, "Emergency Fund", new BigDecimal("5000.00"),
                TODAY.plusDays(100), TODAY.minusDays(10));
    }

    @Test
    void get_calculatesProgressPercentageAndRemaining() {
        SavingsGoal goal = goalOwnedBy(1L);
        when(goalRepository.findById(7L)).thenReturn(Optional.of(goal));
        when(transactionRepository.sumSince(eq(1L), eq(CategoryType.INCOME), eq(TODAY.minusDays(10))))
                .thenReturn(new BigDecimal("1500.00"));
        when(transactionRepository.sumSince(eq(1L), eq(CategoryType.EXPENSE), eq(TODAY.minusDays(10))))
                .thenReturn(new BigDecimal("500.00"));

        GoalResponse response = goalService.get(1L, 7L);

        assertEquals(0, new BigDecimal("1000.00").compareTo(response.currentProgress()));
        assertEquals(0, new BigDecimal("20.00").compareTo(response.progressPercentage()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(response.remainingAmount()));
    }

    @Test
    void get_withNoTransactions_treatsNullSumsAsZero() {
        SavingsGoal goal = goalOwnedBy(1L);
        when(goalRepository.findById(7L)).thenReturn(Optional.of(goal));
        when(transactionRepository.sumSince(eq(1L), any(CategoryType.class), any(LocalDate.class))).thenReturn(null);

        GoalResponse response = goalService.get(1L, 7L);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.currentProgress()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(response.remainingAmount()));
    }

    @Test
    void get_otherUsersGoal_throwsForbidden() {
        SavingsGoal goal = goalOwnedBy(2L);
        when(goalRepository.findById(7L)).thenReturn(Optional.of(goal));
        ApiException ex = assertThrows(ApiException.class, () -> goalService.get(1L, 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void get_missingGoal_throwsNotFound() {
        when(goalRepository.findById(7L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> goalService.get(1L, 7L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void create_pastTargetDate_throwsBadRequest() {
        GoalRequest request = new GoalRequest("Goal", new BigDecimal("100"), TODAY.minusDays(1), null);
        ApiException ex = assertThrows(ApiException.class, () -> goalService.create(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(goalRepository, never()).save(any());
    }

    @Test
    void create_defaultsStartDateToClockToday() {
        when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.getReferenceById(1L)).thenReturn(owner);
        GoalRequest request = new GoalRequest("Goal", new BigDecimal("100"), TODAY.plusDays(5), null);
        GoalResponse response = goalService.create(1L, request);
        assertEquals(TODAY, response.startDate());
    }
}
