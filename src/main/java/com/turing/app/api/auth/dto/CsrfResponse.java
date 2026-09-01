package com.turing.app.api.auth.dto;

public record CsrfResponse(String headerName, String parameterName, String token) {}
