package com.turing.app.api.application.service;

import com.turing.app.api.application.dto.AdminStatisticsResponse;
import com.turing.app.api.application.entity.ApplicationStatus;
import com.turing.app.api.application.repository.ApplicationRepository;
import com.turing.app.api.scholarship.entity.PeriodStatus;
import com.turing.app.api.scholarship.repository.ApplicationPeriodRepository;
import com.turing.app.api.scholarship.repository.ScholarshipProgramRepository;
import com.turing.app.api.user.entity.Role;
import com.turing.app.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatisticsService {
  private final UserRepository users;
  private final ApplicationRepository applications;
  private final ScholarshipProgramRepository programs;
  private final ApplicationPeriodRepository periods;

  public AdminStatisticsService(
      UserRepository users,
      ApplicationRepository applications,
      ScholarshipProgramRepository programs,
      ApplicationPeriodRepository periods) {
    this.users = users;
    this.applications = applications;
    this.programs = programs;
    this.periods = periods;
  }

  @Transactional(readOnly = true)
  public AdminStatisticsResponse get() {
    return new AdminStatisticsResponse(
        users.countByRole(Role.USER),
        applications.countDistinctApplicants(),
        applications.count(),
        applications.countByStatus(ApplicationStatus.DRAFT),
        applications.countByStatus(ApplicationStatus.SUBMITTED),
        applications.countByStatus(ApplicationStatus.UNDER_REVIEW),
        applications.countByStatus(ApplicationStatus.APPROVED),
        applications.countByStatus(ApplicationStatus.REJECTED),
        applications.countByStatus(ApplicationStatus.WAITLISTED),
        applications.averageCompletion(),
        programs.countByActiveTrue(),
        periods.countByStatus(PeriodStatus.SCHEDULED),
        periods.countByStatus(PeriodStatus.OPEN));
  }
}
