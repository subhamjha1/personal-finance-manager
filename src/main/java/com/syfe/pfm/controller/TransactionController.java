package com.syfe.pfm.controller;

import com.syfe.pfm.dto.MessageResponse;
import com.syfe.pfm.dto.TransactionDtos.TransactionListResponse;
import com.syfe.pfm.dto.TransactionDtos.TransactionRequest;
import com.syfe.pfm.dto.TransactionDtos.TransactionResponse;
import com.syfe.pfm.dto.TransactionDtos.TransactionUpdateRequest;
import com.syfe.pfm.security.UserPrincipal;
import com.syfe.pfm.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Transaction endpoints. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(principal.getId(), request);
    }

    /** Optional filters: startDate/endDate (inclusive, ISO yyyy-MM-dd), categoryId, category (name), type. */
    @GetMapping
    public TransactionListResponse list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return transactionService.list(principal.getId(), startDate, endDate, categoryId, category, type);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
                                      @Valid @RequestBody TransactionUpdateRequest request) {
        return transactionService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        transactionService.delete(principal.getId(), id);
        return new MessageResponse("Transaction deleted successfully");
    }
}
