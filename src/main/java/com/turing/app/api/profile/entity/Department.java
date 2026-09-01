package com.turing.app.api.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "departments")
public class Department {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "university_id") private University university;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 200) private String faculty;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Department() {}
    public UUID getId() { return id; }
    public University getUniversity() { return university; }
    public String getName() { return name; }
    public String getFaculty() { return faculty; }
    public boolean isActive() { return active; }
}
