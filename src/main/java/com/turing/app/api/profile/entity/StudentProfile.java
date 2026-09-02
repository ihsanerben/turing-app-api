package com.turing.app.api.profile.entity;

import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {
  @Id private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @Column(name = "national_id_encrypted")
  private byte[] nationalIdEncrypted;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 32)
  private String phone;

  @Column(name = "address_line", length = 300)
  private String addressLine;

  @Column(length = 100)
  private String city;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(name = "country_code", length = 2)
  private String countryCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "university_id")
  private University university;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private Department department;

  @Column(name = "other_university", length = 200)
  private String otherUniversity;

  @Column(name = "other_department", length = 200)
  private String otherDepartment;

  @Enumerated(EnumType.STRING)
  @Column(name = "education_level", length = 32)
  private EducationLevel educationLevel;

  @Column(name = "study_year")
  private Integer studyYear;

  @Column(precision = 4, scale = 2)
  private BigDecimal gpa;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected StudentProfile() {}

  public static StudentProfile create(User user, Instant now) {
    StudentProfile value = new StudentProfile();
    value.id = UUID.randomUUID();
    value.user = user;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void update(
      byte[] nationalId,
      LocalDate birthDate,
      String phone,
      String addressLine,
      String city,
      String postalCode,
      String countryCode,
      University university,
      Department department,
      String otherUniversity,
      String otherDepartment,
      EducationLevel educationLevel,
      Integer studyYear,
      BigDecimal gpa,
      Instant now) {
    this.nationalIdEncrypted = nationalId;
    this.birthDate = birthDate;
    this.phone = phone;
    this.addressLine = addressLine;
    this.city = city;
    this.postalCode = postalCode;
    this.countryCode = countryCode;
    this.university = university;
    this.department = department;
    this.otherUniversity = otherUniversity;
    this.otherDepartment = otherDepartment;
    this.educationLevel = educationLevel;
    this.studyYear = studyYear;
    this.gpa = gpa;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public byte[] getNationalIdEncrypted() {
    return nationalIdEncrypted;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getCity() {
    return city;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public University getUniversity() {
    return university;
  }

  public Department getDepartment() {
    return department;
  }

  public String getOtherUniversity() {
    return otherUniversity;
  }

  public String getOtherDepartment() {
    return otherDepartment;
  }

  public EducationLevel getEducationLevel() {
    return educationLevel;
  }

  public Integer getStudyYear() {
    return studyYear;
  }

  public BigDecimal getGpa() {
    return gpa;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
