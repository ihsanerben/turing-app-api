package com.turing.app.api.application.controller;

import com.turing.app.api.application.dto.AdminStatisticsResponse;
import com.turing.app.api.application.service.AdminStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {
  private final AdminStatisticsService service;

  public AdminStatisticsController(AdminStatisticsService service) {
    this.service = service;
  }

  @GetMapping
  public AdminStatisticsResponse get() {
    return service.get();
  }
}
