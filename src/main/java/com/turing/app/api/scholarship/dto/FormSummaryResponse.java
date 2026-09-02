package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.*;
import java.time.Instant;
import java.util.UUID;

public record FormSummaryResponse(UUID id,UUID periodId,String name,int versionNumber,FormStatus status,Instant publishedAt,long version){public static FormSummaryResponse from(FormDefinition value){return new FormSummaryResponse(value.getId(),value.getPeriod().getId(),value.getName(),value.getVersionNumber(),value.getStatus(),value.getPublishedAt(),value.getVersion());}}
