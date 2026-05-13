package com.malek.owner_service.Dtos.StaffDto;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull Long newPrimaryOwnerUserId
) {}