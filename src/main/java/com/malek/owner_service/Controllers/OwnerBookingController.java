package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.OwnerDto.OwnerBookingResponse;
import com.malek.owner_service.Services.OwnerBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor

public class OwnerBookingController {
    private final OwnerBookingService bookingService;

    @GetMapping("/owner/facilities/{facilityId}/bookings")
    public List<OwnerBookingResponse> getSchedule(@PathVariable Long facilityId, Authentication auth) {
        return bookingService.getFacilitySchedule(facilityId, userId(auth));
    }
    @GetMapping("/owner/bookings/{id}")
    public OwnerBookingResponse getBooking(@PathVariable Long id, Authentication auth) {
        return bookingService.getBooking(id,userId(auth));
    }
    @PostMapping("/owner/bookings/{id}/seat")
    public OwnerBookingResponse markSeated(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication auth

    ) {
        requireKey(idempotencyKey);
        return bookingService.markSeated(id,idempotencyKey,userId(auth));

    }
    @PostMapping("/owner/bookings/{id}/complete")
    public OwnerBookingResponse markCompleted(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication auth
    ) {
        requireKey(idempotencyKey);
        return bookingService.markCompleted(id,idempotencyKey,userId(auth));
    }
    @PostMapping("/owner/bookings/{id}/no-show")
    public OwnerBookingResponse markNoShow(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication auth
    ) {
        requireKey(idempotencyKey);
        return bookingService.markNoShow(id,idempotencyKey,userId(auth));
    }
    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header required");
        }
    }

    private static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}







