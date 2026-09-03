package com.turing.app.api.notification.mail;

public interface EmailSender {
  void send(String email, String subject, String body);

  default void send(
      String email, String subject, String body, String attachmentName, byte[] attachment) {
    send(email, subject, body);
  }
}
