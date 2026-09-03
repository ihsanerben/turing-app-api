package com.turing.app.api.user.controller;

import com.turing.app.api.user.dto.AdminUserResponse;
import com.turing.app.api.user.entity.Role;
import com.turing.app.api.user.service.AdminUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
  private final AdminUserService service;

  public AdminUserController(AdminUserService service) {
    this.service = service;
  }

  @GetMapping("/students")
  public List<AdminUserResponse> students() {
    return service.list(Role.USER);
  }

  @GetMapping("/admins")
  public List<AdminUserResponse> admins() {
    return service.list(Role.ADMIN);
  }

  @GetMapping("/{id}")
  public AdminUserResponse get(@PathVariable UUID id) {
    return service.get(id);
  }
}
