package com.turing.app.api.profile.dto;
import com.turing.app.api.profile.entity.Department;
import java.util.UUID;
public record DepartmentResponse(UUID id, UUID universityId, String name, String faculty) {
    public static DepartmentResponse from(Department value) { return new DepartmentResponse(value.getId(), value.getUniversity().getId(), value.getName(), value.getFaculty()); }
}
