package com.turing.app.api.scholarship.repository;
import com.turing.app.api.scholarship.entity.ScholarshipProgram;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScholarshipProgramRepository extends JpaRepository<ScholarshipProgram,UUID>{ boolean existsBySlugIgnoreCase(String slug); Optional<ScholarshipProgram> findBySlugIgnoreCase(String slug); Optional<ScholarshipProgram> findBySlugAndActiveTrue(String slug); List<ScholarshipProgram> findAllByOrderByNameAsc(); List<ScholarshipProgram> findByActiveTrueOrderByNameAsc(); }
