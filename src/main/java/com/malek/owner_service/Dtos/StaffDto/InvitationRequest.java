package com.malek.owner_service.Dtos.StaffDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InvitationRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^(OWNER|MANAGER|STAFF)$") String role
) {}
