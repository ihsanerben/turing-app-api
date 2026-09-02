package com.turing.app.api.notification.mail;

public interface EmailSender {
  void send(String email, String subject, String body);
}
