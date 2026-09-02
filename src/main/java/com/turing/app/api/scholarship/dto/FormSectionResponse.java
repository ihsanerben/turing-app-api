package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.FormSection;
import java.util.*;

public record FormSectionResponse(UUID id,String title,String description,int order,List<FormFieldResponse> fields){public static FormSectionResponse from(FormSection value){return new FormSectionResponse(value.getId(),value.getTitle(),value.getDescription(),value.getDisplayOrder(),value.getFields().stream().map(FormFieldResponse::from).toList());}}
