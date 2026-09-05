package com.turing.app.api.participation.exception;

import org.springframework.http.HttpStatus;

public class ParticipationException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public ParticipationException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }
}
