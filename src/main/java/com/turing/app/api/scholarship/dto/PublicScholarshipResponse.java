package com.turing.app.api.scholarship.dto;
import java.util.List;
public record PublicScholarshipResponse(ProgramResponse program,List<PeriodResponse> periods){}
