package com.turing.app.api.document.repository;

import com.turing.app.api.document.entity.DocumentRequirement;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, UUID> {
  List<DocumentRequirement> findByPeriodIdOrderByDisplayOrderAsc(UUID periodId);

  boolean existsByPeriodIdAndNameIgnoreCase(UUID periodId, String name);
}
