package com.turing.app.api.document.entity;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
public class StoredFile {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id")
  private User owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id")
  private Application application;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requirement_id")
  private DocumentRequirement requirement;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 20)
  private String provider;

  @Column(name = "checksum_sha256", nullable = false, length = 64)
  private String checksumSha256;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private FileStatus status;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected StoredFile() {}

  public static StoredFile create(
      User owner,
      Application app,
      DocumentRequirement requirement,
      String name,
      String key,
      String mime,
      long size,
      String checksum,
      Instant now) {
    StoredFile value = new StoredFile();
    value.id = UUID.randomUUID();
    value.owner = owner;
    value.application = app;
    value.requirement = requirement;
    value.originalName = name;
    value.storageKey = key;
    value.mimeType = mime;
    value.sizeBytes = size;
    value.provider = "MINIO";
    value.checksumSha256 = checksum;
    value.status = FileStatus.ACTIVE;
    value.uploadedAt = now;
    value.createdAt = now;
    value.updatedAt = now;
    return value;
  }

  public void replace(Instant now) {
    status = FileStatus.REPLACED;
    updatedAt = now;
  }

  public void delete(Instant now) {
    status = FileStatus.DELETED;
    deletedAt = now;
    updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public User getOwner() {
    return owner;
  }

  public Application getApplication() {
    return application;
  }

  public DocumentRequirement getRequirement() {
    return requirement;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getChecksumSha256() {
    return checksumSha256;
  }

  public FileStatus getStatus() {
    return status;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public long getVersion() {
    return version;
  }
}
