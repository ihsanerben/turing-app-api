package com.turing.app.api.application.repository;

import com.turing.app.api.application.entity.ApplicationAnswer;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, UUID> {
  List<ApplicationAnswer> findByApplicationIdOrderByFieldId(UUID applicationId);

  @Modifying
  @Query("delete from ApplicationAnswer a where a.application.id=:applicationId")
  void deleteByApplicationId(@Param("applicationId") UUID applicationId);
}
