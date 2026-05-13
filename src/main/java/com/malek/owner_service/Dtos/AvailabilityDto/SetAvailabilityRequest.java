package com.malek.owner_service.Dtos.AvailabilityDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetAvailabilityRequest (
        @NotNull @Valid List<AvailabilitySlot> slots
) {}
