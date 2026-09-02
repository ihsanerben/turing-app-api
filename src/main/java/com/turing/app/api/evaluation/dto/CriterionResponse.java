package com.turing.app.api.evaluation.dto;
import com.turing.app.api.evaluation.entity.EvaluationCriterion;import java.math.BigDecimal;import java.util.UUID;
public record CriterionResponse(UUID id,UUID periodId,String name,String description,BigDecimal maxScore,BigDecimal weight,int displayOrder,long version){public static CriterionResponse from(EvaluationCriterion v){return new CriterionResponse(v.getId(),v.getPeriod().getId(),v.getName(),v.getDescription(),v.getMaxScore(),v.getWeight(),v.getDisplayOrder(),v.getVersion());}}
