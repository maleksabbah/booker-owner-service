package com.malek.owner_service.Dtos.AvailabilityDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AvailabilitySlot (
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt
) {}

