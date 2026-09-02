package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_periods")
public class ApplicationPeriod {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id")
  private ScholarshipProgram program;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "academic_year", nullable = false, length = 9)
  private String academicYear;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PeriodStatus status;

  @Column(name = "max_recipients")
  private Integer maxRecipients;

  @Column(name = "allow_withdrawal", nullable = false)
  private boolean allowWithdrawal;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected ApplicationPeriod() {}

  public static ApplicationPeriod create(
      ScholarshipProgram program,
      String name,
      String year,
      Instant start,
      Instant end,
      Integer max,
      boolean withdrawal,
      Instant now) {
    ApplicationPeriod value = new ApplicationPeriod();
    value.id = UUID.randomUUID();
    value.program = program;
    value.status = PeriodStatus.DRAFT;
    value.createdAt = now;
    value.update(name, year, start, end, max, withdrawal, now);
    return value;
  }

  public void update(
      String name,
      String year,
      Instant start,
      Instant end,
      Integer max,
      boolean withdrawal,
      Instant now) {
    this.name = name;
    this.academicYear = year;
    this.startsAt = start;
    this.endsAt = end;
    this.maxRecipients = max;
    this.allowWithdrawal = withdrawal;
    this.updatedAt = now;
  }

  public void transition(PeriodStatus next, Instant now) {
    status = next;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public ScholarshipProgram getProgram() {
    return program;
  }

  public String getName() {
    return name;
  }

  public String getAcademicYear() {
    return academicYear;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public PeriodStatus getStatus() {
    return status;
  }

  public Integer getMaxRecipients() {
    return maxRecipients;
  }

  public boolean isAllowWithdrawal() {
    return allowWithdrawal;
  }

  public long getVersion() {
    return version;
  }
}
