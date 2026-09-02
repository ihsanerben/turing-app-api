package com.turing.app.api.application.repository;

import com.turing.app.api.application.entity.Application;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application,UUID>{
    List<Application> findByProfileUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Application> findByIdAndProfileUserId(UUID id,UUID userId);
    boolean existsByProfileIdAndPeriodId(UUID profileId,UUID periodId);
}
