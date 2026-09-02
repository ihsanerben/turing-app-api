package com.turing.app.api.scholarship.exception;

import org.springframework.http.HttpStatus;

public class ScholarshipException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public ScholarshipException(HttpStatus status, String code, String message) {
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
