package com.turing.app.api.config.repository;

import com.turing.app.api.config.entity.AppConfig;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, UUID> {}
