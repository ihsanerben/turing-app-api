package com.turing.app.api.profile.repository;

import com.turing.app.api.profile.entity.University;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, UUID> {
  List<University> findByActiveTrueOrderByNameAsc();
}
