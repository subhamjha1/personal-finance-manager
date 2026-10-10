package com.syfe.pfm.dto;

import com.syfe.pfm.entity.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response bodies of the transaction endpoints. */
public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotNull(message = "amount is required")
            @DecimalMin(value = "0", inclusive = false, message = "amount must be greater than 0")
            @Digits(integer = 15, fraction = 2, message = "amount must have at most 15 integer and 2 fraction digits")
            BigDecimal amount,

            @NotNull(message = "date is required (YYYY-MM-DD)")
            LocalDate date,

            @NotBlank(message = "category is required")
            String category,

            @Size(max = 500, message = "description must be at most 500 characters")
            String description) {
    }

    /** All fields optional; omitted fields keep their value. {@code date} is accepted only to be rejected if it differs. */
    public record TransactionUpdateRequest(
            @DecimalMin(value = "0", inclusive = false, message = "amount must be greater than 0")
            @Digits(integer = 15, fraction = 2, message = "amount must have at most 15 integer and 2 fraction digits")
            BigDecimal amount,

            LocalDate date,

            String category,

            @Size(max = 500, message = "description must be at most 500 characters")
            String description) {
    }

    public record TransactionResponse(Long id, BigDecimal amount, LocalDate date, String category,
                                      String description, CategoryType type) {
    }

    public record TransactionListResponse(List<TransactionResponse> transactions) {
    }
}
