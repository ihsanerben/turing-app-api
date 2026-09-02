package com.turing.app.api.notification.mail;

import com.turing.app.api.auth.security.AuthProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
  private final JavaMailSender sender;
  private final AuthProperties properties;

  public SmtpEmailSender(JavaMailSender sender, AuthProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  public void send(String email, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(properties.mailFrom());
    message.setTo(email);
    message.setSubject(subject);
    message.setText(body);
    sender.send(message);
  }
}
