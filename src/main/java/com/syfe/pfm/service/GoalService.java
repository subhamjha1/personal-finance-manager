package com.syfe.pfm.service;

import com.syfe.pfm.dto.GoalDtos.GoalListResponse;
import com.syfe.pfm.dto.GoalDtos.GoalRequest;
import com.syfe.pfm.dto.GoalDtos.GoalResponse;
import com.syfe.pfm.dto.GoalDtos.GoalUpdateRequest;
import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.entity.SavingsGoal;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.SavingsGoalRepository;
import com.syfe.pfm.repository.TransactionRepository;
import com.syfe.pfm.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Savings goals. Progress is recalculated from persisted transactions on every read:
 * {@code currentProgress = income - expenses} for transactions dated on or after the goal's start date
 * (start date inclusive). Values are not capped: progress may be negative or exceed 100%.
 */
@Service
public class GoalService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public GoalService(SavingsGoalRepository goalRepository, TransactionRepository transactionRepository,
                       UserRepository userRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Creates a goal; start date defaults to today, target date must be in the future and after the start. */
    @Transactional
    public GoalResponse create(Long userId, GoalRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = request.startDate() != null ? request.startDate() : today;
        validateTargetDate(request.targetDate(), today, start);
        SavingsGoal goal = new SavingsGoal(userRepository.getReferenceById(userId), request.goalName().trim(),
                request.targetAmount().setScale(2, RoundingMode.HALF_UP), request.targetDate(), start);
        return toResponse(userId, goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public GoalListResponse list(Long userId) {
        return new GoalListResponse(goalRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(g -> toResponse(userId, g)).toList());
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long userId, Long id) {
        return toResponse(userId, findOwned(userId, id));
    }

    /** Updates targetAmount / targetDate (and optionally goalName); omitted fields are preserved. */
    @Transactional
    public GoalResponse update(Long userId, Long id, GoalUpdateRequest request) {
        SavingsGoal goal = findOwned(userId, id);
        if (request.goalName() != null) {
            if (request.goalName().isBlank()) {
                throw ApiException.badRequest("goalName must not be blank");
            }
            goal.setGoalName(request.goalName().trim());
        }
        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.targetDate() != null) {
            validateTargetDate(request.targetDate(), LocalDate.now(clock), goal.getStartDate());
            goal.setTargetDate(request.targetDate());
        }
        return toResponse(userId, goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        goalRepository.delete(findOwned(userId, id));
    }

    /** 404 if the goal does not exist, 403 if it belongs to another user. */
    private SavingsGoal findOwned(Long userId, Long id) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Goal " + id + " not found"));
        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("You do not have access to this goal");
        }
        return goal;
    }

    private static void validateTargetDate(LocalDate target, LocalDate today, LocalDate start) {
        if (!target.isAfter(today)) {
            throw ApiException.badRequest("targetDate must be in the future");
        }
        if (!target.isAfter(start)) {
            throw ApiException.badRequest("targetDate must be after startDate");
        }
    }

    GoalResponse toResponse(Long userId, SavingsGoal goal) {
        BigDecimal income = orZero(transactionRepository.sumSince(userId, CategoryType.INCOME, goal.getStartDate()));
        BigDecimal expenses = orZero(transactionRepository.sumSince(userId, CategoryType.EXPENSE, goal.getStartDate()));
        BigDecimal progress = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        BigDecimal target = goal.getTargetAmount();
        BigDecimal percentage = progress.multiply(HUNDRED).divide(target, 2, RoundingMode.HALF_UP);
        BigDecimal remaining = target.subtract(progress).setScale(2, RoundingMode.HALF_UP);
        return new GoalResponse(goal.getId(), goal.getGoalName(), target, goal.getTargetDate(), goal.getStartDate(),
                progress, percentage, remaining);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
