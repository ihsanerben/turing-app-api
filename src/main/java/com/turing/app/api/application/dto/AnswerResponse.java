package com.turing.app.api.application.dto;

import com.turing.app.api.application.entity.ApplicationAnswer;
import java.util.UUID;

public record AnswerResponse(UUID fieldId, Object value) {
  public static AnswerResponse from(ApplicationAnswer answer) {
    return new AnswerResponse(answer.getField().getId(), answer.getValue());
  }
}
