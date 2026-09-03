package com.turing.app.api.notification.mail;

import com.turing.app.api.auth.security.AuthProperties;
import com.turing.app.api.notification.exception.NotificationException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
  private final JavaMailSender sender;
  private final AuthProperties properties;

  public SmtpEmailSender(JavaMailSender sender, AuthProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  public void send(
      String email, String subject, String body, String attachmentName, byte[] attachment) {
    try {
      var message = sender.createMimeMessage();
      var helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
      helper.setFrom(properties.mailFrom());
      helper.setTo(email);
      helper.setSubject(subject);
      helper.setText(body);
      if (attachment != null)
        helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
      sender.send(message);
    } catch (Exception exception) {
      throw new NotificationException(
          HttpStatus.BAD_GATEWAY, "EMAIL_SEND_FAILED", "E-posta sunucusu iletiyi gönderemedi.");
    }
  }

  @Override
  public void send(String email, String subject, String body) {
    send(email, subject, body, null, null);
  }
}
