package com.turing.app.api.content.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faq_items")
public class FaqItem {
  @Id private UUID id;

  @Column(nullable = false, length = 500)
  private String question;

  @Column(nullable = false, columnDefinition = "text")
  private String answer;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected FaqItem() {}

  public static FaqItem create(String question, String answer, int order, Instant now) {
    FaqItem v = new FaqItem();
    v.id = UUID.randomUUID();
    v.active = true;
    v.createdAt = now;
    v.update(question, answer, order, now);
    return v;
  }

  public void update(String question, String answer, int order, Instant now) {
    this.question = question;
    this.answer = answer;
    displayOrder = order;
    updatedAt = now;
  }

  public void archive(Instant now) {
    active = false;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getQuestion() {
    return question;
  }

  public String getAnswer() {
    return answer;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public boolean isActive() {
    return active;
  }

  public long getVersion() {
    return version;
  }
}
