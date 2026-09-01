package com.turing.app.api.profile.repository;
import com.turing.app.api.profile.entity.Department;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DepartmentRepository extends JpaRepository<Department, UUID> { List<Department> findByUniversityIdAndActiveTrueOrderByNameAsc(UUID universityId); }
