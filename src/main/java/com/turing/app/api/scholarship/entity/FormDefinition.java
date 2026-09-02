package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "forms")
public class FormDefinition {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "period_id")
  private ApplicationPeriod period;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private FormStatus status;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<FormSection> sections = new ArrayList<>();

  protected FormDefinition() {}

  public static FormDefinition create(
      ApplicationPeriod period, String name, int number, Instant now) {
    FormDefinition value = new FormDefinition();
    value.id = UUID.randomUUID();
    value.period = period;
    value.name = name;
    value.versionNumber = number;
    value.status = FormStatus.DRAFT;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void replaceSchema(String name, List<FormSection> values, Instant now) {
    this.name = name;
    sections.clear();
    values.forEach(
        section -> {
          section.attach(this);
          sections.add(section);
        });
    updatedAt = now;
  }

  public void publish(Instant now) {
    status = FormStatus.PUBLISHED;
    publishedAt = now;
    updatedAt = now;
  }

  public void retire(Instant now) {
    status = FormStatus.RETIRED;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public ApplicationPeriod getPeriod() {
    return period;
  }

  public String getName() {
    return name;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public FormStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public long getVersion() {
    return version;
  }

  public List<FormSection> getSections() {
    return sections;
  }
}
