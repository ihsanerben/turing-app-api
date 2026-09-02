package com.turing.app.api.scholarship.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "form_sections")
public class FormSection {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "form_id")
  private FormDefinition form;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 1000)
  private String description;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<FormField> fields = new ArrayList<>();

  protected FormSection() {}

  public static FormSection create(
      String title, String description, int order, List<FormField> values, Instant now) {
    FormSection section = new FormSection();
    section.id = UUID.randomUUID();
    section.title = title;
    section.description = description;
    section.displayOrder = order;
    section.createdAt = now;
    section.updatedAt = now;
    values.forEach(
        field -> {
          field.attach(section);
          section.fields.add(field);
        });
    return section;
  }

  void attach(FormDefinition value) {
    form = value;
    fields.forEach(field -> field.attachForm(value));
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public List<FormField> getFields() {
    return fields;
  }
}
