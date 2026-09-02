package com.turing.app.api.auth.service;

import com.turing.app.api.auth.security.AuthProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpAuthMailService implements AuthMailService {
  private final JavaMailSender sender;
  private final AuthProperties properties;

  public SmtpAuthMailService(JavaMailSender sender, AuthProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  @Override
  public void sendVerification(String email, String token) {
    send(
        email,
        "E-posta adresinizi doğrulayın",
        properties.frontendBaseUrl() + "/verify-email?token=" + token);
  }

  @Override
  public void sendPasswordReset(String email, String token) {
    send(
        email,
        "Şifrenizi yenileyin",
        properties.frontendBaseUrl() + "/reset-password?token=" + token);
  }

  private void send(String email, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(properties.mailFrom());
    message.setTo(email);
    message.setSubject(subject);
    message.setText(body);
    sender.send(message);
  }
}
