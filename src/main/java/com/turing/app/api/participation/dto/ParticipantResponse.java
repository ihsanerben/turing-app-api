package com.turing.app.api.participation.dto;

import java.util.UUID;

public record ParticipantResponse(UUID userId, String firstName, String lastName) {}
