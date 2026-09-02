package com.turing.app.api.auth.dto;

import com.turing.app.api.user.entity.User;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, String role) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getRole().name());
  }
}
