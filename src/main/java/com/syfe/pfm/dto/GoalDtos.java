package com.syfe.pfm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response bodies of the savings-goal endpoints. */
public final class GoalDtos {

    private GoalDtos() {
    }

    public record GoalRequest(
            @NotBlank(message = "goalName is required")
            @Size(max = 100, message = "goalName must be at most 100 characters")
            String goalName,

            @NotNull(message = "targetAmount is required")
            @DecimalMin(value = "0", inclusive = false, message = "targetAmount must be greater than 0")
            @Digits(integer = 15, fraction = 2, message = "targetAmount must have at most 15 integer and 2 fraction digits")
            BigDecimal targetAmount,

            @NotNull(message = "targetDate is required (YYYY-MM-DD)")
            LocalDate targetDate,

            LocalDate startDate) {
    }

    /** Partial update: omitted fields are preserved. The start date cannot be changed. */
    public record GoalUpdateRequest(
            @Size(max = 100, message = "goalName must be at most 100 characters")
            String goalName,

            @DecimalMin(value = "0", inclusive = false, message = "targetAmount must be greater than 0")
            @Digits(integer = 15, fraction = 2, message = "targetAmount must have at most 15 integer and 2 fraction digits")
            BigDecimal targetAmount,

            LocalDate targetDate) {
    }

    public record GoalResponse(Long id, String goalName, BigDecimal targetAmount, LocalDate targetDate,
                               LocalDate startDate, BigDecimal currentProgress, BigDecimal progressPercentage,
                               BigDecimal remainingAmount) {
    }

    public record GoalListResponse(List<GoalResponse> goals) {
    }
}
