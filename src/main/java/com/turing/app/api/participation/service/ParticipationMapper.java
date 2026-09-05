package com.turing.app.api.participation.service;

import com.turing.app.api.participation.dto.ActivityResponse;
import com.turing.app.api.participation.repository.ParticipationRepository.Activity;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class ParticipationMapper {
  public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
  private final Clock clock;

  public ParticipationMapper(Clock clock) {
    this.clock = clock;
  }

  public boolean registrationOpen(Activity value) {
    Instant deadline =
        value.date() == null ? value.startsAt() : value.date().atStartOfDay(ZONE).toInstant();
    return clock.instant().isBefore(deadline);
  }

  public ActivityResponse response(Activity value) {
    return new ActivityResponse(
        value.id(),
        value.title(),
        value.date() == null ? value.description() : "",
        value.date(),
        value.startsAt(),
        value.location(),
        value.attending(),
        registrationOpen(value),
        value.version());
  }
}
