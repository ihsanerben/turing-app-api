package com.turing.app.api.participation.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, String sort) {}
