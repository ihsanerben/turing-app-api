package com.turing.app.api.application.dto;

public record AdminStatisticsResponse(
    long registeredCandidates,
    long candidatesWithApplication,
    long totalApplications,
    long pendingApplications,
    long missingDocumentApplications,
    long approvedApplications,
    long rejectedApplications,
    double averageCompletion,
    long activePrograms,
    long scheduledPeriods,
    long openPeriods) {}
