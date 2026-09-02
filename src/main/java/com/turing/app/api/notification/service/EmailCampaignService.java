package com.turing.app.api.notification.service;

import static com.turing.app.api.notification.dto.NotificationDtos.*;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.notification.exception.NotificationException;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.sql.ResultSet;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailCampaignService {
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final EmailCampaignProcessor processor;
  private final Executor executor;
  private final AuditService audit;
  private final ObjectMapper json;
  private final Clock clock;

  public EmailCampaignService(
      JdbcTemplate jdbc,
      UserRepository users,
      EmailCampaignProcessor processor,
      @Qualifier("emailExecutor") Executor executor,
      AuditService audit,
      ObjectMapper json,
      Clock clock) {
    this.jdbc = jdbc;
    this.users = users;
    this.processor = processor;
    this.executor = executor;
    this.audit = audit;
    this.json = json;
    this.clock = clock;
  }

  @Transactional
  public CampaignDetail create(UUID actor, CampaignRequest request, String ip) {
    List<User> recipients = users.findAllById(request.userIds());
    if (recipients.size() != request.userIds().size())
      throw error(
          HttpStatus.BAD_REQUEST,
          "CAMPAIGN_RECIPIENT_NOT_FOUND",
          "Bir veya daha fazla alıcı bulunamadı.");
    UUID id = UUID.randomUUID();
    OffsetDateTime now = now();
    jdbc.update(
        "insert into email_campaigns(id,subject,body,status,created_by,created_at,updated_at,version)values(?,?,?,'DRAFT',?,?,?,0)",
        id,
        request.subject().trim(),
        request.body().trim(),
        actor,
        now,
        now);
    for (User user : recipients)
      jdbc.update(
          "insert into email_recipients(id,campaign_id,user_id,email,status,updated_at,version)values(?,?,?,?,'PENDING',?,0)",
          UUID.randomUUID(),
          id,
          user.getId(),
          user.getEmail(),
          now);
    audit.record(
        actor,
        "EMAIL_CAMPAIGN_CREATED",
        "EMAIL_CAMPAIGN",
        id,
        "{}",
        write(Map.of("recipientCount", recipients.size())),
        ip);
    return detail(id);
  }

  @Transactional(readOnly = true)
  public List<CampaignSummary> list() {
    return jdbc.query(
        "select c.*,count(r.id) recipient_count,count(r.id) filter(where r.status='SENT') sent_count,count(r.id) filter(where r.status='FAILED') failed_count from email_campaigns c left join email_recipients r on r.campaign_id=c.id group by c.id order by c.created_at desc",
        (rs, n) -> summary(rs));
  }

  @Transactional(readOnly = true)
  public CampaignDetail detail(UUID id) {
    var campaigns =
        jdbc.query(
            "select * from email_campaigns where id=?",
            (rs, n) ->
                new CampaignDetail(
                    rs.getObject("id", UUID.class),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("status"),
                    List.of(),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getLong("version")),
            id);
    if (campaigns.isEmpty())
      throw error(HttpStatus.NOT_FOUND, "CAMPAIGN_NOT_FOUND", "E-posta kampanyası bulunamadı.");
    List<Recipient> recipients =
        jdbc.query(
            "select * from email_recipients where campaign_id=? order by email",
            (rs, n) -> recipient(rs),
            id);
    CampaignDetail c = campaigns.getFirst();
    return new CampaignDetail(
        c.id(), c.subject(), c.body(), c.status(), recipients, c.createdAt(), c.version());
  }

  @Transactional
  public CampaignDetail send(UUID actor, UUID id, long version, String ip) {
    int changed =
        jdbc.update(
            "update email_campaigns set status='SENDING',updated_at=?,version=version+1 where id=? and version=? and status='DRAFT'",
            now(),
            id,
            version);
    if (changed == 0) transitionFailure(id, version, "Yalnız taslak kampanya gönderilebilir.");
    audit.record(actor, "EMAIL_CAMPAIGN_SENT", "EMAIL_CAMPAIGN", id, "{}", "{}", ip);
    afterCommit(id);
    return detail(id);
  }

  @Transactional
  public CampaignDetail retry(UUID actor, UUID id, long version, String ip) {
    int changed =
        jdbc.update(
            "update email_campaigns set status='SENDING',updated_at=?,version=version+1 where id=? and version=? and status='COMPLETED' and exists(select 1 from email_recipients where campaign_id=? and status='FAILED')",
            now(),
            id,
            version,
            id);
    if (changed == 0)
      transitionFailure(
          id, version, "Yalnız tamamlanmış ve başarısız alıcısı olan kampanya tekrar denenebilir.");
    jdbc.update(
        "update email_recipients set status='PENDING',failure_message=null,updated_at=?,version=version+1 where campaign_id=? and status='FAILED'",
        now(),
        id);
    audit.record(actor, "EMAIL_CAMPAIGN_RETRIED", "EMAIL_CAMPAIGN", id, "{}", "{}", ip);
    afterCommit(id);
    return detail(id);
  }

  private void afterCommit(UUID id) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            executor.execute(() -> processor.process(id));
          }
        });
  }

  private void transitionFailure(UUID id, long version, String message) {
    List<Map<String, Object>> row =
        jdbc.queryForList("select version from email_campaigns where id=?", id);
    if (row.isEmpty())
      throw error(HttpStatus.NOT_FOUND, "CAMPAIGN_NOT_FOUND", "E-posta kampanyası bulunamadı.");
    if (((Number) row.getFirst().get("version")).longValue() != version)
      throw error(
          HttpStatus.CONFLICT,
          "CAMPAIGN_VERSION_CONFLICT",
          "Kampanya başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
    throw error(HttpStatus.CONFLICT, "CAMPAIGN_INVALID_STATUS", message);
  }

  private CampaignSummary summary(ResultSet rs) throws java.sql.SQLException {
    return new CampaignSummary(
        rs.getObject("id", UUID.class),
        rs.getString("subject"),
        rs.getString("status"),
        rs.getInt("recipient_count"),
        rs.getInt("sent_count"),
        rs.getInt("failed_count"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getLong("version"));
  }

  private Recipient recipient(ResultSet rs) throws java.sql.SQLException {
    var sent = rs.getTimestamp("sent_at");
    return new Recipient(
        rs.getObject("id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getString("email"),
        rs.getString("status"),
        rs.getInt("attempt_count"),
        rs.getString("failure_message"),
        sent == null ? null : sent.toInstant());
  }

  private NotificationException error(HttpStatus s, String c, String m) {
    return new NotificationException(s, c, m);
  }

  private String write(Object value) {
    return json.writeValueAsString(value);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }
}
