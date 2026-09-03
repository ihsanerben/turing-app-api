package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_field_options")
public class FormFieldOption {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "field_id")
  private FormField field;

  @Column(nullable = false, length = 200)
  private String label;

  @Column(name = "option_value", nullable = false, length = 100)
  private String value;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FormFieldOption() {}

  public static FormFieldOption create(String label, String value, int order, Instant now) {
    FormFieldOption option = new FormFieldOption();
    option.id = UUID.randomUUID();
    option.label = label;
    option.value = value;
    option.displayOrder = order;
    option.createdAt = now;
    option.updatedAt = now;
    return option;
  }

  public void attach(FormField value) {
    field = value;
  }

  public void update(String label, String value, int order, Instant now) {
    this.label = label;
    this.value = value;
    this.displayOrder = order;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }
}
