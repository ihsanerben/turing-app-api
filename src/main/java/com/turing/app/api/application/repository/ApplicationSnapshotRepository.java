package com.turing.app.api.application.repository;

import com.turing.app.api.application.entity.ApplicationSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSnapshotRepository extends JpaRepository<ApplicationSnapshot, UUID> {
  boolean existsByApplicationId(UUID applicationId);
}
