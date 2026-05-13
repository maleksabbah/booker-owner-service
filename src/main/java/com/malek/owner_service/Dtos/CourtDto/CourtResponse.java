package com.malek.owner_service.Dtos.CourtDto;

import java.math.BigDecimal;
import java.time.Instant;

public record CourtResponse(
        Long id,
        Long facilityId,
        String name,
        String sport,
        String surface,
        Integer capacity,
        BigDecimal pricePerHour,
        String currency,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}