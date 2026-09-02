package com.turing.app.api.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnswersUpdateRequest(@NotNull Long version,@NotNull List<@Valid AnswerRequest> answers) {}
