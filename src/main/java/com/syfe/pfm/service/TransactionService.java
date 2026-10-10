package com.syfe.pfm.service;

import com.syfe.pfm.dto.TransactionDtos.TransactionListResponse;
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
import jakarta.persistence.criteria.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transaction CRUD. Every lookup is scoped by the authenticated user's id. */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository,
                              UserRepository userRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Creates a transaction; type is derived from the category. */
    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        if (request.date().isAfter(LocalDate.now(clock))) {
            throw ApiException.badRequest("Transaction date cannot be in the future");
        }
        Category category = resolveCategory(userId, request.category());
        Transaction tx = new Transaction(userRepository.getReferenceById(userId), category,
                scale(request.amount()), request.date(), request.description());
        return toResponse(transactionRepository.save(tx));
    }

    /**
     * Lists the user's transactions, newest first (date desc, then id desc). Date bounds are inclusive.
     *
     * @param type optional "INCOME"/"EXPENSE" (case-insensitive)
     */
    @Transactional(readOnly = true)
    public TransactionListResponse list(Long userId, LocalDate startDate, LocalDate endDate, Long categoryId,
                                        String categoryName, String type) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw ApiException.badRequest("startDate must not be after endDate");
        }
        CategoryType categoryType = parseType(type);

        Specification<Transaction> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        if (startDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<LocalDate>get("transactionDate"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.<LocalDate>get("transactionDate"), endDate));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (categoryName != null && !categoryName.isBlank()) {
            String lowered = categoryName.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> {
                Path<String> name = root.get("category").get("name");
                return cb.equal(cb.lower(name), lowered);
            });
        }
        if (categoryType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("categoryType"), categoryType));
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        List<TransactionResponse> items = transactionRepository.findAll(spec, sort).stream()
                .map(TransactionService::toResponse).toList();
        return new TransactionListResponse(items);
    }

    /**
     * Partially updates a transaction. Omitted (null) fields are preserved. The date can never change:
     * a supplied date different from the stored one is rejected with 400.
     */
    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionUpdateRequest request) {
        Transaction tx = findOwned(userId, id);
        if (request.date() != null && !request.date().equals(tx.getTransactionDate())) {
            throw ApiException.badRequest("Transaction date cannot be modified");
        }
        if (request.amount() != null) {
            tx.setAmount(scale(request.amount()));
        }
        if (request.category() != null) {
            if (request.category().isBlank()) {
                throw ApiException.badRequest("category must not be blank");
            }
            tx.setCategory(resolveCategory(userId, request.category()));
        }
        if (request.description() != null) {
            tx.setDescription(request.description());
        }
        return toResponse(transactionRepository.save(tx));
    }

    /** Deletes a transaction; goals and reports are computed on demand so they reflect it immediately. */
    @Transactional
    public void delete(Long userId, Long id) {
        transactionRepository.delete(findOwned(userId, id));
    }

    private Transaction findOwned(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction " + id + " not found"));
    }

    private Category resolveCategory(Long userId, String name) {
        return categoryRepository.findVisibleByName(name.trim(), userId)
                .orElseThrow(() -> ApiException.badRequest("Category '" + name + "' does not exist or is not accessible"));
    }

    private static CategoryType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return CategoryType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("type must be INCOME or EXPENSE");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(tx.getId(), tx.getAmount(), tx.getTransactionDate(),
                tx.getCategory().getName(), tx.getDescription(), tx.getCategory().getType());
    }
}
