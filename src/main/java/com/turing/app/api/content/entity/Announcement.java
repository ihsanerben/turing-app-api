package com.turing.app.api.content.entity;

import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
public class Announcement {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 200)
  private String slug;

  @Column(nullable = false, length = 500)
  private String summary;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AnnouncementStatus status;

  @Column(name = "published_at")
  private Instant publishedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected Announcement() {}

  public static Announcement create(
      String title, String slug, String summary, String content, User actor, Instant now) {
    Announcement v = new Announcement();
    v.id = UUID.randomUUID();
    v.status = AnnouncementStatus.DRAFT;
    v.createdBy = actor;
    v.createdAt = now;
    v.update(title, slug, summary, content, now);
    return v;
  }

  public void update(String title, String slug, String summary, String content, Instant now) {
    this.title = title;
    this.slug = slug;
    this.summary = summary;
    this.content = content;
    updatedAt = now;
  }

  public void publish(Instant now) {
    status = AnnouncementStatus.PUBLISHED;
    publishedAt = now;
    updatedAt = now;
  }

  public void archive(Instant now) {
    status = AnnouncementStatus.ARCHIVED;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getSlug() {
    return slug;
  }

  public String getSummary() {
    return summary;
  }

  public String getContent() {
    return content;
  }

  public AnnouncementStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }
}
