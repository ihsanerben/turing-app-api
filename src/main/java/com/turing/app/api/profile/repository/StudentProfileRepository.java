package com.turing.app.api.profile.repository;
import com.turing.app.api.profile.entity.StudentProfile;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> { Optional<StudentProfile> findByUserId(UUID userId); }
