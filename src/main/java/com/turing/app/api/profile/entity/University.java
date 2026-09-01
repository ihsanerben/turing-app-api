package com.turing.app.api.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "universities")
public class University {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected University() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCountryCode() { return countryCode; }
    public boolean isActive() { return active; }
}
