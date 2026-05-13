package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.BlackoutDto.BlackoutRequest;
import com.malek.owner_service.Dtos.BlackoutDto.BlackoutResponse;
import com.malek.owner_service.Services.BlackoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BlackoutController {

    private final BlackoutService blackoutService;

    @PostMapping("/owner/courts/{courtId}/blackouts")
    public BlackoutResponse addBlackout(
            @PathVariable Long courtId,
            @Valid @RequestBody BlackoutRequest req,
            Authentication auth) {
        return blackoutService.addBlackout(courtId, req, userId(auth));
    }

    @GetMapping("/owner/courts/{courtId}/blackouts")
    public List<BlackoutResponse> listBlackouts(@PathVariable Long courtId, Authentication auth) {
        return blackoutService.listBlackouts(courtId, userId(auth));
    }

    @DeleteMapping("/owner/blackouts/{id}")
    public ResponseEntity<Void> deleteBlackout(@PathVariable Long id, Authentication auth) {
        blackoutService.deleteBlackout(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
