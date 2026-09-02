package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "scholarship_programs")
public class ScholarshipProgram {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, unique = true, length = 200) private String slug;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;
    protected ScholarshipProgram() {}
    public static ScholarshipProgram create(String name, String slug, String description, Instant now) {
        ScholarshipProgram value = new ScholarshipProgram(); value.id = UUID.randomUUID(); value.active = true;
        value.update(name, slug, description, now); value.createdAt = now; return value;
    }
    public void update(String name, String slug, String description, Instant now) { this.name=name; this.slug=slug; this.description=description; this.updatedAt=now; }
    public void archive(Instant now) { active=false; updatedAt=now; }
    public UUID getId(){return id;} public String getName(){return name;} public String getSlug(){return slug;}
    public String getDescription(){return description;} public boolean isActive(){return active;} public long getVersion(){return version;}
}
