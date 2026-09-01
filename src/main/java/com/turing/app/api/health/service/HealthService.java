package com.turing.app.api.health.service;

import com.turing.app.api.health.dto.HealthResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthResponse getHealth() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        String databaseStatus = Integer.valueOf(1).equals(result) ? "up" : "down";
        return new HealthResponse("ok", databaseStatus);
    }
}
