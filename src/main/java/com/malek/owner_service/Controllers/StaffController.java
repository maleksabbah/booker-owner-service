package com.malek.owner_service.Controllers;

import com.malek.owner_service.Dtos.StaffDto.ChangeRoleRequest;
import com.malek.owner_service.Dtos.StaffDto.InvitationRequest;
import com.malek.owner_service.Dtos.StaffDto.InvitationResponse;
import com.malek.owner_service.Dtos.StaffDto.MemberResponse;
import com.malek.owner_service.Dtos.StaffDto.TransferOwnershipRequest;
import com.malek.owner_service.Dtos.StaffDto.InvitationResponse;
import com.malek.owner_service.Services.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor

public class StaffController {
    private final StaffService staffService;

    // ── Invitations ──────────────────────────────────────────────
    @PostMapping("/owner/facilities/{facilityId}/invitations")
    public InvitationResponse inviteMember(
            @PathVariable Long facilityId,
            @Valid @RequestBody InvitationRequest req,
            Authentication auth) {
        return staffService.inviteMember(facilityId, req, userId(auth));
    }

    @GetMapping("/owner/facilities/{facilityId}/invitations")
    public List<InvitationResponse> listInvitations(
            @PathVariable Long facilityId,
            Authentication auth) {
        return staffService.listInvitations(facilityId, userId(auth));
    }

    @DeleteMapping("/owner/invitations/{id}")
    public ResponseEntity<Void> revokeInvitation(@PathVariable Long id, Authentication auth) {
        staffService.revokeInvitation(id, userId(auth));
        return ResponseEntity.noContent().build();
    }
    // ── Members ──────────────────────────────────────────────────

    @GetMapping("/owner/facilities/{facilityId}/members")
    public List<MemberResponse> listMembers(@PathVariable Long facilityId, Authentication auth) {
        return staffService.listMembers(facilityId, userId(auth));
    }

    @PatchMapping("/owner/facilities/{facilityId}/members/{userId}")
    public MemberResponse changeMemberRole(
            @PathVariable Long facilityId,
            @PathVariable("userId") Long targetUserId,
            @RequestBody @Valid ChangeRoleRequest req,
            Authentication auth) {
        return staffService.changeMemberRole(facilityId, targetUserId, req, callerId(auth));

    }

    @DeleteMapping("/owner/facilities/{facilityId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long facilityId,
            @PathVariable("userId") Long targetUserId,
            Authentication auth) {
        staffService.removeMember(facilityId, targetUserId, callerId(auth));
        return ResponseEntity.noContent().build();
    }

    // ── Transfer ownership ───────────────────────────────────────
    @PostMapping("/owner/facilities/{facilityId}/transfer-ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long facilityId,
            @Valid @RequestBody TransferOwnershipRequest req,
            Authentication auth) {
        staffService.transferOwnership(facilityId, req, callerId(auth));
        return ResponseEntity.noContent().build();

    }

    // ── helpers ──────────────────────────────────────────────────
    public static Long userId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }

    public static Long callerId(Authentication auth) {
        return Long.valueOf(auth.getName());
    }
}
