package com.turing.app.api.application.dto;

import com.turing.app.api.scholarship.dto.FormResponse;
import java.util.List;

public record ApplicationFormResponse(
    ApplicationResponse application, FormResponse form, List<AnswerResponse> answers) {}
