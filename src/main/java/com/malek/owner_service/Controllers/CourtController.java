package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.CourtDto.CourtRequest;
import com.malek.owner_service.Dtos.CourtDto.CourtResponse;
import com.malek.owner_service.Services.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @PostMapping("/owner/facilities/{facilityId}/courts")
    public CourtResponse createCourt(
            @PathVariable Long facilityId,
            @Valid @RequestBody CourtRequest req,
            Authentication auth) {
        return courtService.createCourt(facilityId, req, userId(auth));
    }

    @GetMapping("/owner/facilities/{facilityId}/courts")
    public List<CourtResponse> listCourtsAtFacility(@PathVariable Long facilityId, Authentication auth) {
        return courtService.listCourtsAtFacility(facilityId, userId(auth));
    }

    @GetMapping("/owner/courts/{id}")
    public CourtResponse getCourt(@PathVariable Long id, Authentication auth) {
        return courtService.getCourt(id, userId(auth));
    }

    @PatchMapping("/owner/courts/{id}")
    public CourtResponse updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequest req,
            Authentication auth) {
        return courtService.updateCourt(id, req, userId(auth));
    }

    @DeleteMapping("/owner/courts/{id}")
    public ResponseEntity<Void> softDeleteCourt(@PathVariable Long id, Authentication auth) {
        courtService.softDeleteCourt(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
