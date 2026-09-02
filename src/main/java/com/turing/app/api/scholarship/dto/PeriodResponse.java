package com.turing.app.api.scholarship.dto;
import com.turing.app.api.scholarship.entity.*;
import java.time.Instant;
import java.util.UUID;
public record PeriodResponse(UUID id,UUID programId,String programName,String name,String academicYear,Instant startsAt,Instant endsAt,PeriodStatus status,Integer maxRecipients,boolean allowWithdrawal,long version){public static PeriodResponse from(ApplicationPeriod v){return new PeriodResponse(v.getId(),v.getProgram().getId(),v.getProgram().getName(),v.getName(),v.getAcademicYear(),v.getStartsAt(),v.getEndsAt(),v.getStatus(),v.getMaxRecipients(),v.isAllowWithdrawal(),v.getVersion());}}
