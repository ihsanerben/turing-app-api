package com.turing.app.api.content.service;

import static com.turing.app.api.content.dto.ContentDtos.*;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.content.entity.*;
import com.turing.app.api.content.exception.ContentException;
import com.turing.app.api.content.repository.*;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ContentService {
  private final AnnouncementRepository announcements;
  private final UserRepository users;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public ContentService(
      AnnouncementRepository announcements,
      UserRepository users,
      AuditService audit,
      ObjectMapper json,
      Clock clock) {
    this.announcements = announcements;
    this.users = users;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> adminAnnouncements() {
    return announcements.findAllByOrderByCreatedAtDesc().stream()
        .map(AnnouncementResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public AnnouncementResponse adminAnnouncement(UUID id) {
    return AnnouncementResponse.from(announcement(id));
  }

  @Transactional
  public AnnouncementResponse createAnnouncement(UUID actorId, AnnouncementRequest r, String ip) {
    uniqueSlug(r.slug(), null);
    User actor = user(actorId);
    Announcement value =
        announcements.saveAndFlush(
            Announcement.create(
                clean(r.title()),
                r.slug(),
                clean(r.summary()),
                clean(r.content()),
                actor,
                clock.instant()));
    audit.record(
        actorId, "ANNOUNCEMENT_CREATED", "ANNOUNCEMENT", value.getId(), "{}", snapshot(value), ip);
    return AnnouncementResponse.from(value);
  }

  @Transactional
  public AnnouncementResponse updateAnnouncement(
      UUID actorId, UUID id, AnnouncementRequest r, String ip) {
    Announcement value = announcement(id);
    version(value.getVersion(), r.version());
    if (value.getStatus() != AnnouncementStatus.DRAFT)
      throw conflict("ANNOUNCEMENT_NOT_EDITABLE", "Yalnız taslak duyuru düzenlenebilir.");
    uniqueSlug(r.slug(), id);
    String old = snapshot(value);
    value.update(
        clean(r.title()), r.slug(), clean(r.summary()), clean(r.content()), clock.instant());
    announcements.flush();
    audit.record(actorId, "ANNOUNCEMENT_UPDATED", "ANNOUNCEMENT", id, old, snapshot(value), ip);
    return AnnouncementResponse.from(value);
  }

  @Transactional
  public AnnouncementResponse publish(UUID actorId, UUID id, long supplied, String ip) {
    Announcement value = announcement(id);
    version(value.getVersion(), supplied);
    if (value.getStatus() != AnnouncementStatus.DRAFT)
      throw conflict("ANNOUNCEMENT_NOT_DRAFT", "Yalnız taslak duyuru yayınlanabilir.");
    String old = snapshot(value);
    value.publish(clock.instant());
    announcements.flush();
    audit.record(actorId, "ANNOUNCEMENT_PUBLISHED", "ANNOUNCEMENT", id, old, snapshot(value), ip);
    return AnnouncementResponse.from(value);
  }

  @Transactional
  public AnnouncementResponse archiveAnnouncement(UUID actorId, UUID id, long supplied, String ip) {
    Announcement value = announcement(id);
    version(value.getVersion(), supplied);
    if (value.getStatus() == AnnouncementStatus.ARCHIVED)
      throw conflict("ANNOUNCEMENT_ARCHIVED", "Duyuru zaten arşivlenmiş.");
    String old = snapshot(value);
    value.archive(clock.instant());
    announcements.flush();
    audit.record(actorId, "ANNOUNCEMENT_ARCHIVED", "ANNOUNCEMENT", id, old, snapshot(value), ip);
    return AnnouncementResponse.from(value);
  }

  @Transactional
  public AnnouncementResponse restoreAnnouncement(UUID actorId, UUID id, long supplied, String ip) {
    Announcement value = announcement(id);
    version(value.getVersion(), supplied);
    if (value.getStatus() != AnnouncementStatus.ARCHIVED)
      throw conflict("ANNOUNCEMENT_NOT_ARCHIVED", "Yalnız arşivdeki duyuru geri alınabilir.");
    String old = snapshot(value);
    value.restore(clock.instant());
    announcements.flush();
    audit.record(actorId, "ANNOUNCEMENT_RESTORED", "ANNOUNCEMENT", id, old, snapshot(value), ip);
    return AnnouncementResponse.from(value);
  }

  @Transactional
  public void deleteAnnouncement(UUID actorId, UUID id, long supplied, String ip) {
    Announcement value = announcement(id);
    version(value.getVersion(), supplied);
    String old = snapshot(value);
    announcements.delete(value);
    announcements.flush();
    audit.record(actorId, "ANNOUNCEMENT_DELETED", "ANNOUNCEMENT", id, old, "{}", ip);
  }

  @Transactional(readOnly = true)
  public List<AnnouncementSummary> publicAnnouncements() {
    return announcements.findByStatusOrderByPublishedAtDesc(AnnouncementStatus.PUBLISHED).stream()
        .map(
            v ->
                new AnnouncementSummary(
                    v.getId(), v.getTitle(), v.getSlug(), v.getSummary(), v.getPublishedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicAnnouncement publicAnnouncement(String slug) {
    Announcement v =
        announcements
            .findBySlugAndStatus(slug, AnnouncementStatus.PUBLISHED)
            .orElseThrow(() -> notFound("ANNOUNCEMENT_NOT_FOUND", "Duyuru bulunamadı."));
    return new PublicAnnouncement(
        v.getId(), v.getTitle(), v.getSlug(), v.getSummary(), v.getContent(), v.getPublishedAt());
  }

  private void uniqueSlug(String slug, UUID id) {
    announcements
        .findBySlugIgnoreCase(slug)
        .filter(v -> !v.getId().equals(id))
        .ifPresent(
            v -> {
              throw conflict("ANNOUNCEMENT_SLUG_EXISTS", "Bu URL adı zaten kullanılıyor.");
            });
  }

  private Announcement announcement(UUID id) {
    return announcements
        .findById(id)
        .orElseThrow(() -> notFound("ANNOUNCEMENT_NOT_FOUND", "Duyuru bulunamadı."));
  }

  private User user(UUID id) {
    return users
        .findById(id)
        .orElseThrow(() -> notFound("USER_NOT_FOUND", "Kullanıcı bulunamadı."));
  }

  private void version(long actual, Long supplied) {
    if (supplied == null || actual != supplied)
      throw conflict(
          "VERSION_CONFLICT", "Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
  }

  private String clean(String v) {
    return v.trim();
  }

  private String snapshot(Announcement v) {
    return json.writeValueAsString(
        Map.of(
            "title",
            v.getTitle(),
            "slug",
            v.getSlug(),
            "status",
            v.getStatus(),
            "version",
            v.getVersion()));
  }

  private ContentException conflict(String c, String m) {
    return new ContentException(HttpStatus.CONFLICT, c, m);
  }

  private ContentException notFound(String c, String m) {
    return new ContentException(HttpStatus.NOT_FOUND, c, m);
  }
}
