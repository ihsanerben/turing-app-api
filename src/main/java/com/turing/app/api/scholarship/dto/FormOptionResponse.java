package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.FormFieldOption;
import java.util.UUID;

public record FormOptionResponse(UUID id,String label,String value,int order){public static FormOptionResponse from(FormFieldOption value){return new FormOptionResponse(value.getId(),value.getLabel(),value.getValue(),value.getDisplayOrder());}}
