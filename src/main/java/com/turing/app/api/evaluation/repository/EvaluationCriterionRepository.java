package com.turing.app.api.evaluation.repository;
import com.turing.app.api.evaluation.entity.EvaluationCriterion;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion,UUID>{List<EvaluationCriterion> findByPeriodIdOrderByDisplayOrderAsc(UUID periodId);boolean existsByPeriodIdAndNameIgnoreCase(UUID periodId,String name);boolean existsByPeriodIdAndDisplayOrder(UUID periodId,int order);}
