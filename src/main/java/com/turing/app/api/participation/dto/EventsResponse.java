package com.turing.app.api.participation.dto;

public record EventsResponse(PageResponse<ActivityResponse> events, long version) {}
