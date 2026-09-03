package com.turing.app.api.notification.service;

import com.turing.app.api.notification.mail.EmailSender;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailCampaignProcessor {
  private final JdbcTemplate jdbc;
  private final EmailSender sender;
  private final TransactionTemplate transactions;
  private final Clock clock;
  private final NotificationService notifications;

  public EmailCampaignProcessor(
      JdbcTemplate jdbc,
      EmailSender sender,
      TransactionTemplate transactions,
      Clock clock,
      NotificationService notifications) {
    this.jdbc = jdbc;
    this.sender = sender;
    this.transactions = transactions;
    this.clock = clock;
    this.notifications = notifications;
  }

  public void process(UUID campaignId) {
    List<UUID> ids =
        jdbc.queryForList(
            "select id from email_recipients where campaign_id=? and status='PENDING' order by email",
            UUID.class,
            campaignId);
    for (UUID id : ids) sendOne(id);
    transactions.executeWithoutResult(
        s ->
            jdbc.update(
                "update email_campaigns set status='COMPLETED',updated_at=?,version=version+1 where id=? and not exists(select 1 from email_recipients where campaign_id=? and status='PENDING')",
                now(),
                campaignId,
                campaignId));
  }

  private void sendOne(UUID id) {
    var row =
        jdbc.queryForMap(
            "select r.user_id,r.email,c.id campaign_id,c.subject,c.body,c.attachment_name,c.attachment_data from email_recipients r join email_campaigns c on c.id=r.campaign_id where r.id=?",
            id);
    String failure = null;
    try {
      sender.send(
          (String) row.get("email"),
          (String) row.get("subject"),
          (String) row.get("body"),
          (String) row.get("attachment_name"),
          (byte[]) row.get("attachment_data"));
    } catch (RuntimeException exception) {
      failure = safe(exception.getMessage());
    }
    String error = failure;
    transactions.executeWithoutResult(
        s -> {
          if (error == null) {
            jdbc.update(
                "update email_recipients set status='SENT',attempt_count=attempt_count+1,failure_message=null,sent_at=?,updated_at=?,version=version+1 where id=? and status='PENDING'",
                now(),
                now(),
                id);
            notifications.create(
                (UUID) row.get("user_id"),
                (String) row.get("subject"),
                (String) row.get("body"),
                "INFO",
                "EMAIL_CAMPAIGN",
                (UUID) row.get("campaign_id"));
          } else
            jdbc.update(
                "update email_recipients set status='FAILED',attempt_count=attempt_count+1,failure_message=?,updated_at=?,version=version+1 where id=? and status='PENDING'",
                error,
                now(),
                id);
        });
  }

  private String safe(String message) {
    if (message == null || message.isBlank()) return "E-posta gönderimi başarısız.";
    return message.length() > 1000 ? message.substring(0, 1000) : message;
  }

  private OffsetDateTime now() {
    return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }
}
