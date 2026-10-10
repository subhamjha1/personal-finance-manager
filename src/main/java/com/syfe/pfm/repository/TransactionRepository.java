package com.syfe.pfm.repository;

import com.syfe.pfm.entity.CategoryType;
import com.syfe.pfm.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence access for {@link Transaction}, including aggregation queries used by goals and reports. */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    /** Sum of amounts of the given type on/after {@code from}; {@code null} when there are no rows. */
    @Query("select sum(t.amount) from Transaction t "
            + "where t.user.id = :userId and t.category.categoryType = :type and t.transactionDate >= :from")
    BigDecimal sumSince(@Param("userId") Long userId, @Param("type") CategoryType type, @Param("from") LocalDate from);

    /** Rows of {category name, sum(amount)} within an inclusive date range. */
    @Query("select t.category.name, sum(t.amount) from Transaction t "
            + "where t.user.id = :userId and t.category.categoryType = :type "
            + "and t.transactionDate >= :from and t.transactionDate <= :to "
            + "group by t.category.name")
    List<Object[]> sumByCategory(@Param("userId") Long userId, @Param("type") CategoryType type,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);
}
