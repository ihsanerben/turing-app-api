package com.turing.app.api.participation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityResponse(
    UUID id,
    String title,
    String description,
    LocalDate date,
    Instant startsAt,
    String location,
    boolean attending,
    boolean registrationOpen,
    long version) {}
