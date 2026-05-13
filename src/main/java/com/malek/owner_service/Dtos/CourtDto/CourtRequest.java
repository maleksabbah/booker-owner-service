package com.malek.owner_service.Dtos.CourtDto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CourtRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) String sport,
        @NotBlank @Size(max = 50) String surface,
        @NotNull @Min(1) Integer capacity,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerHour,
        @NotBlank @Size(max = 3) String currency
) {
}

