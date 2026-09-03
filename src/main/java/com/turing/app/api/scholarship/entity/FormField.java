package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "form_fields")
public class FormField {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id")
  private FormDefinition form;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "section_id")
  private FormSection section;

  @Column(name = "field_key", nullable = false, length = 80)
  private String key;

  @Column(nullable = false, length = 250)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 20)
  private FormFieldType type;

  @Column(nullable = false)
  private boolean required;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(length = 250)
  private String placeholder;

  @Column(name = "requirement_id")
  private UUID requirementId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "validation_rules", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> validationRules = new LinkedHashMap<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<FormFieldOption> options = new ArrayList<>();

  protected FormField() {}

  public static FormField create(
      String key,
      String label,
      FormFieldType type,
      boolean required,
      int order,
      String placeholder,
      UUID requirementId,
      Map<String, Object> rules,
      List<FormFieldOption> values,
      Instant now) {
    FormField field = new FormField();
    field.id = UUID.randomUUID();
    field.key = key;
    field.label = label;
    field.type = type;
    field.required = required;
    field.displayOrder = order;
    field.placeholder = placeholder;
    field.requirementId = requirementId;
    field.validationRules = new LinkedHashMap<>(rules);
    field.createdAt = now;
    field.updatedAt = now;
    values.forEach(
        option -> {
          option.attach(field);
          field.options.add(option);
        });
    return field;
  }

  public void attach(FormSection value) {
    section = value;
  }

  public void attachForm(FormDefinition value) {
    form = value;
  }

  public void update(
      String key,
      String label,
      FormFieldType type,
      boolean required,
      int order,
      String placeholder,
      UUID requirementId,
      Map<String, Object> rules,
      Instant now) {
    this.key = key;
    this.label = label;
    this.type = type;
    this.required = required;
    this.displayOrder = order;
    this.placeholder = placeholder;
    this.requirementId = requirementId;
    this.validationRules = new LinkedHashMap<>(rules);
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getLabel() {
    return label;
  }

  public FormFieldType getType() {
    return type;
  }

  public boolean isRequired() {
    return required;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getPlaceholder() {
    return placeholder;
  }

  public UUID getRequirementId() {
    return requirementId;
  }

  public Map<String, Object> getValidationRules() {
    return validationRules;
  }

  public List<FormFieldOption> getOptions() {
    return options;
  }
}
