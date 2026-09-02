package com.turing.app.api.application.repository;

import com.turing.app.api.application.entity.ApplicationStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory,UUID>{}
