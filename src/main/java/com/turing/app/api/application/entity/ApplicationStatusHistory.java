package com.turing.app.api.application.entity;

import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_status_history")
public class ApplicationStatusHistory {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id")
  private Application application;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", length = 24)
  private ApplicationStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 24)
  private ApplicationStatus newStatus;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "changed_by")
  private User changedBy;

  @Column(length = 500)
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ApplicationStatusHistory() {}

  public static ApplicationStatusHistory create(
      Application app,
      ApplicationStatus oldStatus,
      ApplicationStatus next,
      User actor,
      String reason,
      Instant now) {
    ApplicationStatusHistory value = new ApplicationStatusHistory();
    value.id = UUID.randomUUID();
    value.application = app;
    value.oldStatus = oldStatus;
    value.newStatus = next;
    value.changedBy = actor;
    value.reason = reason;
    value.createdAt = now;
    return value;
  }

  public UUID getId() {
    return id;
  }

  public ApplicationStatus getOldStatus() {
    return oldStatus;
  }

  public ApplicationStatus getNewStatus() {
    return newStatus;
  }

  public User getChangedBy() {
    return changedBy;
  }

  public String getReason() {
    return reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
