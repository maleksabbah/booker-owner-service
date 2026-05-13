package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.FacilityDto.FacilityRequest;
import com.malek.owner_service.Dtos.FacilityDto.FacilityResponse;
import com.malek.owner_service.Services.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping
    public FacilityResponse createFacility(@Valid @RequestBody FacilityRequest req, Authentication auth) {
        return facilityService.createFacility(req, userId(auth));
    }

    @GetMapping
    public List<FacilityResponse> listMyFacilities(Authentication auth) {
        return facilityService.listMyFacilities(userId(auth));
    }

    @GetMapping("/{id}")
    public FacilityResponse getFacility(@PathVariable Long id, Authentication auth) {
        return facilityService.getFacility(id, userId(auth));
    }

    @PatchMapping("/{id}")
    public FacilityResponse updateFacility(
            @PathVariable Long id,
            @Valid @RequestBody FacilityRequest req,
            Authentication auth) {
        return facilityService.updateFacility(id, req, userId(auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteFacility(@PathVariable Long id, Authentication auth) {
        facilityService.softDeleteFacility(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
