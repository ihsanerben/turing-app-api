package com.turing.app.api.scholarship.repository;

import com.turing.app.api.scholarship.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {
  List<FormDefinition> findByPeriodIdOrderByVersionNumberDesc(UUID periodId);

  Optional<FormDefinition> findByPeriodIdAndStatus(UUID periodId, FormStatus status);

  boolean existsByPeriodIdAndStatus(UUID periodId, FormStatus status);

  @Query(
      "select coalesce(max(f.versionNumber),0) from FormDefinition f where f.period.id=:periodId")
  int maxVersion(@Param("periodId") UUID periodId);
}
