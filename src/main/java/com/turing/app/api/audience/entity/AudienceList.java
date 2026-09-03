package com.turing.app.api.audience.entity;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.scholarship.entity.ScholarshipProgram;
import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "audience_lists")
public class AudienceList {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id")
  private ScholarshipProgram program;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToMany
  @JoinTable(
      name = "audience_list_members",
      joinColumns = @JoinColumn(name = "list_id"),
      inverseJoinColumns = @JoinColumn(name = "application_id"))
  @OrderBy("createdAt DESC")
  private Set<Application> applications = new LinkedHashSet<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected AudienceList() {}

  public static AudienceList create(
      String name,
      ScholarshipProgram program,
      User actor,
      Collection<Application> applications,
      Instant now) {
    AudienceList value = new AudienceList();
    value.id = UUID.randomUUID();
    value.name = name;
    value.program = program;
    value.createdBy = actor;
    value.applications.addAll(applications);
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void update(String name, Collection<Application> applications, Instant now) {
    this.name = name;
    this.applications.clear();
    this.applications.addAll(applications);
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public ScholarshipProgram getProgram() {
    return program;
  }

  public Set<Application> getApplications() {
    return applications;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }
}
