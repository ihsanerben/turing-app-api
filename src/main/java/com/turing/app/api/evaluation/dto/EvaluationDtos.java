package com.turing.app.api.evaluation.dto;

import java.math.BigDecimal;
import java.util.*;

public final class EvaluationDtos {
  private EvaluationDtos() {}

  public record Score(
      UUID id,
      UUID criterionId,
      String criterionName,
      BigDecimal maxScore,
      BigDecimal weight,
      UUID reviewerId,
      String reviewerName,
      BigDecimal score,
      String comment,
      long version) {}

  public record ApplicationEvaluation(
      UUID applicationId, BigDecimal weightedTotal, List<Score> scores) {}

  public record Ranking(
      int rank,
      UUID applicationId,
      String studentName,
      String studentEmail,
      BigDecimal weightedTotal) {}
}
