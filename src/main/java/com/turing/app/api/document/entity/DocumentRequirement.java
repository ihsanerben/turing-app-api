package com.turing.app.api.document.entity;

import com.turing.app.api.scholarship.entity.ApplicationPeriod;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_requirements")
public class DocumentRequirement {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "period_id")
  private ApplicationPeriod period;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false)
  private boolean required;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_mime_types", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedMimeTypes;

  @Column(name = "max_size_bytes", nullable = false)
  private long maxSizeBytes;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DocumentRequirement() {}

  public static DocumentRequirement create(
      ApplicationPeriod period,
      String name,
      String description,
      boolean required,
      List<String> mime,
      long max,
      int order,
      Instant now) {
    DocumentRequirement value = new DocumentRequirement();
    value.id = UUID.randomUUID();
    value.period = period;
    value.name = name;
    value.description = description;
    value.required = required;
    value.allowedMimeTypes = List.copyOf(mime);
    value.maxSizeBytes = max;
    value.displayOrder = order;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void update(
      String name,
      String description,
      boolean required,
      List<String> mime,
      long max,
      int order,
      Instant now) {
    this.name = name;
    this.description = description;
    this.required = required;
    this.allowedMimeTypes = List.copyOf(mime);
    this.maxSizeBytes = max;
    this.displayOrder = order;
    this.updatedAt = now;
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

  public String getDescription() {
    return description;
  }

  public boolean isRequired() {
    return required;
  }

  public List<String> getAllowedMimeTypes() {
    return allowedMimeTypes;
  }

  public long getMaxSizeBytes() {
    return maxSizeBytes;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }
}
