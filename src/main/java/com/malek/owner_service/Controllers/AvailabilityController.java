package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.AvailabilityDto.AvailabilityResponse;
import com.malek.owner_service.Dtos.AvailabilityDto.SetAvailabilityRequest;
import com.malek.owner_service.Services.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/courts/{courtId}/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public List<AvailabilityResponse> getSchedule(@PathVariable Long courtId, Authentication auth) {
        return availabilityService.getSchedule(courtId, userId(auth));
    }

    @PutMapping
    public List<AvailabilityResponse> replaceSchedule(
            @PathVariable Long courtId,
            @Valid @RequestBody SetAvailabilityRequest req,
            Authentication auth) {
        return availabilityService.replaceSchedule(courtId, req, userId(auth));
    }

    private static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
