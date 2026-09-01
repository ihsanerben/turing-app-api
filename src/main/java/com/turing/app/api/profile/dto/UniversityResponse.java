package com.turing.app.api.profile.dto;
import com.turing.app.api.profile.entity.University;
import java.util.UUID;
public record UniversityResponse(UUID id, String name, String countryCode) {
    public static UniversityResponse from(University value) { return new UniversityResponse(value.getId(), value.getName(), value.getCountryCode()); }
}
