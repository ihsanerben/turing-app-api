package com.turing.app.api.application.dto;

public record AdminStatisticsResponse(
    long registeredCandidates,
    long candidatesWithApplication,
    long totalApplications,
    long draftApplications,
    long submittedApplications,
    long applicationsUnderReview,
    long approvedApplications,
    long rejectedApplications,
    long waitlistedApplications,
    double averageCompletion,
    long activePrograms,
    long scheduledPeriods,
    long openPeriods) {}
