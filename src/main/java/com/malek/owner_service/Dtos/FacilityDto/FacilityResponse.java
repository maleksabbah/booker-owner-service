package com.malek.owner_service.Dtos.FacilityDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

public record FacilityResponse(
        Long id,                  // 1
        Long ownerUserId,         // 2
        String name,              // 3
        String description,       // 4
        String address,           // 5
        String city,              // 6
        BigDecimal latitude,      // 7
        BigDecimal longitude,     // 8
        String phone,             // 9
        String email,             // 10
        String website,           // 11
        LocalTime opensAt,        // 12
        LocalTime closesAt,       // 13
        Boolean active,           // 14
        Instant createdAt,        // 15
        Instant updatedAt         // 16
) {}
