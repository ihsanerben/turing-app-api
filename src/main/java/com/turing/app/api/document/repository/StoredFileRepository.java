package com.turing.app.api.document.repository;

import com.turing.app.api.document.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
  List<StoredFile> findByApplicationIdAndStatusOrderByRequirementDisplayOrderAsc(
      UUID applicationId, FileStatus status);

  Optional<StoredFile> findByApplicationIdAndRequirementIdAndStatus(
      UUID applicationId, UUID requirementId, FileStatus status);

  Optional<StoredFile> findByIdAndOwnerId(UUID id, UUID ownerId);

  long countByApplicationIdAndRequirementIdInAndStatus(
      UUID applicationId, Collection<UUID> requirementIds, FileStatus status);
}
