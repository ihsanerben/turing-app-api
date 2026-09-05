package com.turing.app.api.participation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.turing.app.api.participation.repository.ParticipationRepository.Activity;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParticipationMapperTest {
  @Test
  void mealsCloseAtTurkeyMidnightAndEventsAtTheirExactStart() {
    Instant midnight = Instant.parse("2026-09-13T21:00:00Z");
    Activity meal =
        new Activity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Yemek",
            "",
            LocalDate.of(2026, 9, 14),
            null,
            "",
            true,
            0);
    assertThat(mapper(midnight.minusNanos(1)).registrationOpen(meal)).isTrue();
    assertThat(mapper(midnight).registrationOpen(meal)).isFalse();
    assertThat(mapper(midnight).response(meal).attending()).isTrue();
    Activity event =
        new Activity(
            UUID.randomUUID(), null, "Gezi", "", null, midnight.plusSeconds(3600), "", false, 0);
    assertThat(mapper(midnight.plusSeconds(3599)).registrationOpen(event)).isTrue();
    assertThat(mapper(midnight.plusSeconds(3600)).registrationOpen(event)).isFalse();
  }

  private ParticipationMapper mapper(Instant now) {
    return new ParticipationMapper(Clock.fixed(now, ZoneOffset.UTC));
  }
}
