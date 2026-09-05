package com.turing.app.api.participation.repository;

import com.turing.app.api.participation.dto.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ParticipationRepository {
  private final JdbcTemplate jdbc;

  public ParticipationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Activity(
      UUID id,
      UUID weekId,
      String title,
      String description,
      LocalDate date,
      Instant startsAt,
      String location,
      boolean attending,
      long version) {}

  public List<MealWeekSummary> weeks(int page, int size) {
    return jdbc.query(
        "select id,week_start from meal_weeks order by week_start desc limit ? offset ?",
        (rs, n) ->
            new MealWeekSummary(
                rs.getObject("id", UUID.class), rs.getObject("week_start", LocalDate.class)),
        size,
        (long) page * size);
  }

  public long weekVersion(UUID id) {
    return jdbc.queryForObject("select version from meal_weeks where id=?", Long.class, id);
  }

  public void lockWeek(UUID id) {
    jdbc.queryForList("select id from meal_weeks where id=? for update", id);
  }

  public void lockActivity(UUID id) {
    jdbc.queryForList("select id from participation_activities where id=? for update", id);
  }

  public void incrementWeekVersion(UUID id) {
    jdbc.update("update meal_weeks set version=version+1 where id=?", id);
  }

  public void removeDay(UUID id) {
    jdbc.update("delete from participation_activities where id=?", id);
  }

  public void updateEvent(UUID id, EventUpdateRequest request) {
    jdbc.update(
        "update participation_activities set title=?,description=?,starts_at=?,location=?,version=version+1 where id=?",
        request.title().trim(),
        request.description().trim(),
        utc(request.startsAt()),
        request.location().trim(),
        id);
  }

  public List<UUID> participantIds(UUID id) {
    return jdbc.queryForList(
        "select user_id from participation_registrations where activity_id=? order by user_id",
        UUID.class,
        id);
  }

  public long weekCount() {
    return jdbc.queryForObject("select count(*) from meal_weeks", Long.class);
  }

  public Optional<MealWeekSummary> week(UUID id) {
    return jdbc
        .query(
            "select id,week_start from meal_weeks where id=?",
            (rs, n) ->
                new MealWeekSummary(
                    rs.getObject("id", UUID.class), rs.getObject("week_start", LocalDate.class)),
            id)
        .stream()
        .findFirst();
  }

  public void createWeek(UUID id, LocalDate start, UUID actor, Instant now) {
    jdbc.update(
        "insert into meal_weeks(id,week_start,created_by,created_at) values(?,?,?,?)",
        id,
        start,
        actor,
        utc(now));
  }

  public void createActivity(
      UUID id,
      UUID weekId,
      String title,
      String description,
      LocalDate date,
      Instant start,
      String location,
      UUID actor,
      Instant now) {
    jdbc.update(
        "insert into participation_activities(id,week_id,title,description,meal_date,starts_at,location,created_by,created_at) values(?,?,?,?,?,?,?,?,?)",
        id,
        weekId,
        title,
        description,
        date,
        start == null ? null : utc(start),
        location,
        actor,
        utc(now));
  }

  private static final String ACTIVITY_SELECT =
      """
      select a.*, exists(select 1 from participation_registrations r where r.activity_id=a.id and r.user_id=?) attending
      from participation_activities a
      """;

  public List<Activity> days(UUID week, UUID user) {
    return jdbc.query(
        ACTIVITY_SELECT + " where a.week_id=? order by a.meal_date",
        (rs, n) -> activity(rs),
        user,
        week);
  }

  public List<Activity> events(UUID user, int page, int size) {
    return jdbc.query(
        ACTIVITY_SELECT
            + " where a.week_id is null order by a.starts_at desc,a.id limit ? offset ?",
        (rs, n) -> activity(rs),
        user,
        size,
        (long) page * size);
  }

  public long eventCount() {
    return jdbc.queryForObject(
        "select count(*) from participation_activities where week_id is null", Long.class);
  }

  public Optional<Activity> activity(UUID id, UUID user) {
    return jdbc.query(ACTIVITY_SELECT + " where a.id=?", (rs, n) -> activity(rs), user, id).stream()
        .findFirst();
  }

  public List<Activity> registeredEvents(UUID user, Instant now) {
    return jdbc.query(
        ACTIVITY_SELECT
            + " where a.week_id is null and a.starts_at>? and exists(select 1 from participation_registrations r where r.activity_id=a.id and r.user_id=?) order by a.starts_at,a.id",
        (rs, n) -> activity(rs),
        user,
        utc(now),
        user);
  }

  public long version(UUID user, String scope) {
    return jdbc
        .query(
            "select version from participation_selection_versions where user_id=? and scope=?",
            (rs, n) -> rs.getLong(1),
            user,
            scope)
        .stream()
        .findFirst()
        .orElse(0L);
  }

  public long lockSelection(UUID user, String scope) {
    jdbc.update(
        "insert into participation_selection_versions(user_id,scope) values(?,?) on conflict do nothing",
        user,
        scope);
    return jdbc.queryForObject(
        "select version from participation_selection_versions where user_id=? and scope=? for update",
        Long.class,
        user,
        scope);
  }

  public void incrementVersion(UUID user, String scope) {
    jdbc.update(
        "update participation_selection_versions set version=version+1 where user_id=? and scope=?",
        user,
        scope);
  }

  public void setAttendance(UUID user, UUID activity, boolean attending, Instant now) {
    if (attending) {
      jdbc.update(
          "insert into participation_registrations(activity_id,user_id,created_at) values(?,?,?) on conflict do nothing",
          activity,
          user,
          utc(now));
    } else {
      jdbc.update(
          "delete from participation_registrations where activity_id=? and user_id=?",
          activity,
          user);
    }
  }

  public List<ParticipantResponse> participants(UUID activity, int page, int size) {
    return jdbc.query(
        "select u.id,u.first_name,u.last_name from participation_registrations r join users u on u.id=r.user_id where r.activity_id=? order by u.first_name,u.last_name,u.id limit ? offset ?",
        (rs, n) ->
            new ParticipantResponse(
                rs.getObject("id", UUID.class),
                rs.getString("first_name"),
                rs.getString("last_name")),
        activity,
        size,
        (long) page * size);
  }

  public long participantCount(UUID activity) {
    return jdbc.queryForObject(
        "select count(*) from participation_registrations where activity_id=?",
        Long.class,
        activity);
  }

  private Activity activity(ResultSet rs) throws SQLException {
    var starts = rs.getTimestamp("starts_at");
    return new Activity(
        rs.getObject("id", UUID.class),
        rs.getObject("week_id", UUID.class),
        rs.getString("title"),
        rs.getString("description"),
        rs.getObject("meal_date", LocalDate.class),
        starts == null ? null : starts.toInstant(),
        rs.getString("location"),
        rs.getBoolean("attending"),
        rs.getLong("version"));
  }

  private OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
