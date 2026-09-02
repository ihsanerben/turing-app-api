package com.turing.app.api.interview.exception;

import org.springframework.http.HttpStatus;

public class InterviewException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public InterviewException(HttpStatus status, String code, String message) {
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
