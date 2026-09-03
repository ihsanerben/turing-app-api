package com.turing.app.api.audience.repository;

import com.turing.app.api.audience.entity.AudienceList;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienceListRepository extends JpaRepository<AudienceList, UUID> {
  List<AudienceList> findAllByOrderByCreatedAtDesc();
}
