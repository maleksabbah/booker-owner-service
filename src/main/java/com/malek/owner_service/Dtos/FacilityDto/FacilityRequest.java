package com.malek.owner_service.Dtos.FacilityDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;


public record FacilityRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 20) String phone,
        @Size(max = 255) String email,
        @Size(max = 255) String website,
        LocalTime opensAt,
        LocalTime closesAt

){
}

