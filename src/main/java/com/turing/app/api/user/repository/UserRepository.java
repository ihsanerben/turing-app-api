package com.turing.app.api.user.repository;

import com.turing.app.api.user.entity.Role;
import com.turing.app.api.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  long countByRole(Role role);

  List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
