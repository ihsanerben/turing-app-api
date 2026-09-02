package com.turing.app.api.evaluation.repository;

import com.turing.app.api.evaluation.entity.EvaluationScore;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, UUID> {
  Optional<EvaluationScore> findByApplicationIdAndCriterionIdAndReviewerId(
      UUID applicationId, UUID criterionId, UUID reviewerId);

  List<EvaluationScore> findByApplicationId(UUID applicationId);

  List<EvaluationScore> findByApplicationPeriodId(UUID periodId);

  boolean existsByCriterionId(UUID criterionId);
}
