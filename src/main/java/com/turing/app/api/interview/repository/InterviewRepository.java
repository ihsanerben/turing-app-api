package com.turing.app.api.interview.repository;

import com.turing.app.api.interview.entity.Interview;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
  List<Interview> findAllByOrderByStartsAtDesc();

  List<Interview> findByApplicationIdOrderByStartsAtDesc(UUID applicationId);

  List<Interview> findByApplicationProfileUserIdOrderByStartsAtDesc(UUID userId);

  Optional<Interview> findByIdAndApplicationProfileUserId(UUID id, UUID userId);
}
