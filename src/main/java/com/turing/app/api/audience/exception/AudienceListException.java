package com.turing.app.api.audience.exception;

import org.springframework.http.HttpStatus;

public class AudienceListException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public AudienceListException(HttpStatus status, String code, String message) {
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
