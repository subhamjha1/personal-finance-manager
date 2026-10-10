package com.syfe.pfm.repository;

import com.syfe.pfm.entity.SavingsGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for {@link SavingsGoal}. */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByIdAsc(Long userId);
}
