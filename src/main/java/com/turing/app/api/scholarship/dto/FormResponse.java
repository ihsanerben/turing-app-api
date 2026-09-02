package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.*;
import java.time.Instant;
import java.util.*;

public record FormResponse(UUID id,UUID periodId,String name,int versionNumber,FormStatus status,Instant publishedAt,long version,List<FormSectionResponse> sections){public static FormResponse from(FormDefinition value){return new FormResponse(value.getId(),value.getPeriod().getId(),value.getName(),value.getVersionNumber(),value.getStatus(),value.getPublishedAt(),value.getVersion(),value.getSections().stream().map(FormSectionResponse::from).toList());}}
