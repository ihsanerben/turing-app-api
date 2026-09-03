package com.turing.app.api.audience.service;

import static com.turing.app.api.audience.dto.AudienceListDtos.*;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.application.repository.ApplicationRepository;
import com.turing.app.api.audience.entity.AudienceList;
import com.turing.app.api.audience.exception.AudienceListException;
import com.turing.app.api.audience.repository.AudienceListRepository;
import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.scholarship.entity.ScholarshipProgram;
import com.turing.app.api.scholarship.repository.ScholarshipProgramRepository;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AudienceListService {
  private final AudienceListRepository lists;
  private final ApplicationRepository applications;
  private final ScholarshipProgramRepository programs;
  private final UserRepository users;
  private final AuditService audit;
  private final Clock clock;

  public AudienceListService(
      AudienceListRepository lists,
      ApplicationRepository applications,
      ScholarshipProgramRepository programs,
      UserRepository users,
      AuditService audit,
      Clock clock) {
    this.lists = lists;
    this.applications = applications;
    this.programs = programs;
    this.users = users;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<Response> all() {
    return lists.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList();
  }

  @Transactional(readOnly = true)
  public Response get(UUID id) {
    return response(entity(id));
  }

  @Transactional
  public Response create(UUID actorId, CreateRequest request, String ip) {
    ScholarshipProgram program =
        programs
            .findById(request.programId())
            .orElseThrow(() -> notFound("PROGRAM_NOT_FOUND", "Program bulunamadı."));
    User actor =
        users.findById(actorId).orElseThrow(() -> notFound("ADMIN_NOT_FOUND", "Admin bulunamadı."));
    List<Application> selected = applications.findAllById(request.applicationIds());
    if (selected.size() != request.applicationIds().size())
      throw bad("APPLICATION_NOT_FOUND", "Seçilen başvurulardan biri bulunamadı.");
    if (selected.stream()
        .anyMatch(value -> !value.getPeriod().getProgram().getId().equals(program.getId())))
      throw bad(
          "APPLICATION_PROGRAM_MISMATCH",
          "Seçilen öğrencilerin tamamı aynı programa ait olmalıdır.");
    AudienceList saved =
        lists.saveAndFlush(
            AudienceList.create(request.name().trim(), program, actor, selected, clock.instant()));
    audit.record(
        actorId,
        "AUDIENCE_LIST_CREATED",
        "AUDIENCE_LIST",
        saved.getId(),
        "{}",
        "{\"memberCount\":" + selected.size() + "}",
        ip);
    return response(saved);
  }

  @Transactional
  public Response update(UUID actorId, UUID id, UpdateRequest request, String ip) {
    AudienceList list = entity(id);
    if (list.getVersion() != request.version())
      throw new AudienceListException(
          HttpStatus.CONFLICT,
          "VERSION_CONFLICT",
          "Liste başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");
    List<Application> selected = applications.findAllById(request.applicationIds());
    if (selected.size() != request.applicationIds().size())
      throw bad("APPLICATION_NOT_FOUND", "Seçilen başvurulardan biri bulunamadı.");
    if (selected.stream()
        .anyMatch(
            value -> !value.getPeriod().getProgram().getId().equals(list.getProgram().getId())))
      throw bad("APPLICATION_PROGRAM_MISMATCH", "Öğrenciler listenin programına ait olmalıdır.");
    list.update(request.name().trim(), selected, clock.instant());
    lists.flush();
    audit.record(
        actorId,
        "AUDIENCE_LIST_UPDATED",
        "AUDIENCE_LIST",
        id,
        "{}",
        "{\"memberCount\":" + selected.size() + "}",
        ip);
    return response(list);
  }

  public AudienceList entity(UUID id) {
    return lists
        .findById(id)
        .orElseThrow(() -> notFound("AUDIENCE_LIST_NOT_FOUND", "Liste bulunamadı."));
  }

  private Response response(AudienceList value) {
    return new Response(
        value.getId(),
        value.getName(),
        value.getProgram().getId(),
        value.getProgram().getName(),
        value.getApplications().stream()
            .map(
                app ->
                    new Member(
                        app.getId(),
                        app.getProfile().getUser().getId(),
                        app.getProfile().getUser().getFirstName()
                            + " "
                            + app.getProfile().getUser().getLastName(),
                        app.getProfile().getUser().getEmail(),
                        Optional.ofNullable(app.getProfile().getUniversity())
                            .map(university -> university.getName())
                            .orElse(app.getProfile().getOtherUniversity()),
                        Optional.ofNullable(app.getProfile().getDepartment())
                            .map(department -> department.getName())
                            .orElse(app.getProfile().getOtherDepartment()),
                        app.getStatus()))
            .toList(),
        value.getCreatedAt(),
        value.getVersion());
  }

  private AudienceListException notFound(String code, String message) {
    return new AudienceListException(HttpStatus.NOT_FOUND, code, message);
  }

  private AudienceListException bad(String code, String message) {
    return new AudienceListException(HttpStatus.BAD_REQUEST, code, message);
  }
}
