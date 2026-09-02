package com.turing.app.api.scholarship.repository;

import com.turing.app.api.scholarship.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationPeriodRepository extends JpaRepository<ApplicationPeriod, UUID> {
  List<ApplicationPeriod> findByProgramIdOrderByStartsAtDesc(UUID programId);

  List<ApplicationPeriod> findByProgramIdAndStatusInOrderByStartsAtDesc(
      UUID id, Collection<PeriodStatus> statuses);
}
