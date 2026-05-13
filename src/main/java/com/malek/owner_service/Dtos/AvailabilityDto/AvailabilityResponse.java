package com.malek.owner_service.Dtos.AvailabilityDto;

import java.time.LocalTime;

public record AvailabilityResponse (
        Long id,
        Long courtId,
        Integer dayOfWeek,
        LocalTime opensAt,
        LocalTime closesAt
) {}

