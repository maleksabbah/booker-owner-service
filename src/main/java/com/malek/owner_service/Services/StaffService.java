package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.StaffDto.ChangeRoleRequest;
import com.malek.owner_service.Dtos.StaffDto.InvitationRequest;
import com.malek.owner_service.Dtos.StaffDto.InvitationResponse;
import com.malek.owner_service.Dtos.StaffDto.MemberResponse;
import com.malek.owner_service.Dtos.StaffDto.TransferOwnershipRequest;
import com.malek.owner_service.Entities.Facility;
import com.malek.owner_service.Entities.FacilityInvitation;
import com.malek.owner_service.Entities.FacilityMember;
import com.malek.owner_service.Entities.User;
import com.malek.owner_service.Repositories.FacilityInvitationRepository;
import com.malek.owner_service.Repositories.FacilityMemberRepository;
import com.malek.owner_service.Repositories.FacilityRepository;
import com.malek.owner_service.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final SecureRandom RNG = new SecureRandom();

    private final FacilityRepository facilityRepository;
    private final FacilityMemberRepository memberRepository;
    private final FacilityInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final FacilityService facilityService;

    // ─── Role helpers ────────────────────────────────────────────────

    private static int rank(String role) {
        return switch (role) {
            case "STAFF"   -> 1;
            case "MANAGER" -> 2;
            case "OWNER"   -> 3;
            default -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown role: " + role);
        };
    }

    /** Caller's role must be strictly higher than the role they're acting on. */
    private void requireCanManage(String actorRole, String targetRole) {
        if (rank(targetRole) >= rank(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manage members below your role level");
        }
    }

    /** Returns the caller's role at this facility, or 403 if they aren't a member. */
    private String myRoleAt(Long facilityId, Long userId) {
        return memberRepository.findByFacility_IdAndUser_Id(facilityId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this facility"))
                .getRole();
    }

    // ─── Invitations ─────────────────────────────────────────────────

    @Transactional
    public InvitationResponse inviteMember(Long facilityId, InvitationRequest req, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        String myRole = myRoleAt(facilityId, callerUserId);
        requireCanManage(myRole, req.role());

        Facility facility = loadFacility(facilityId);

        FacilityInvitation invite = FacilityInvitation.builder()
                .facility(facility)
                .email(req.email().toLowerCase())
                .role(req.role())
                .token(generateToken())
                .invitedByUserId(callerUserId)
                .expiresAt(Instant.now().plus(INVITATION_TTL))
                .build();

        return toInvitationResponse(invitationRepository.save(invite));
    }

    public List<InvitationResponse> listInvitations(Long facilityId, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        return invitationRepository
                .findByFacility_IdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        facilityId, Instant.now())
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public void revokeInvitation(Long invitationId, Long callerUserId) {
        FacilityInvitation invite = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        Long facilityId = invite.getFacility().getId();

        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        String myRole = myRoleAt(facilityId, callerUserId);
        requireCanManage(myRole, invite.getRole());

        if (invite.getAcceptedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already accepted");
        }
        invite.setRevokedAt(Instant.now());
        invitationRepository.save(invite);
    }

    // ─── Members ─────────────────────────────────────────────────────

    public List<MemberResponse> listMembers(Long facilityId, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "STAFF");
        return memberRepository.findByFacility_IdOrderByAddedAtAsc(facilityId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public MemberResponse changeMemberRole(
            Long facilityId, Long targetUserId, ChangeRoleRequest req, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        String myRole = myRoleAt(facilityId, callerUserId);

        Facility facility = loadFacility(facilityId);
        if (facility.getOwnerUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change role of the primary owner — transfer ownership first");
        }

        FacilityMember member = memberRepository.findByFacility_IdAndUser_Id(facilityId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        // Both the existing role and the new role must be below the caller's role
        requireCanManage(myRole, member.getRole());
        requireCanManage(myRole, req.role());

        // Don't demote the last OWNER
        if ("OWNER".equals(member.getRole()) && !"OWNER".equals(req.role())) {
            long ownerCount = memberRepository.countByFacility_IdAndRole(facilityId, "OWNER");
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last OWNER");
            }
        }

        member.setRole(req.role());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long facilityId, Long targetUserId, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        String myRole = myRoleAt(facilityId, callerUserId);

        Facility facility = loadFacility(facilityId);
        if (facility.getOwnerUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot remove the primary owner — transfer ownership first");
        }

        FacilityMember member = memberRepository.findByFacility_IdAndUser_Id(facilityId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        requireCanManage(myRole, member.getRole());

        if ("OWNER".equals(member.getRole())) {
            long ownerCount = memberRepository.countByFacility_IdAndRole(facilityId, "OWNER");
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last OWNER");
            }
        }
        memberRepository.delete(member);
    }

    // ─── Transfer ownership ──────────────────────────────────────────

    @Transactional
    public void transferOwnership(Long facilityId, TransferOwnershipRequest req, Long callerUserId) {
        Facility facility = loadFacility(facilityId);

        // Only the current primary owner can transfer
        if (!facility.getOwnerUserId().equals(callerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the primary owner can transfer ownership");
        }

        FacilityMember newPrimary = memberRepository
                .findByFacility_IdAndUser_Id(facilityId, req.newPrimaryOwnerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Target user is not a member of this facility"));

        if (!"OWNER".equals(newPrimary.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Target user must be an OWNER member before transfer");
        }

        facility.setOwnerUserId(req.newPrimaryOwnerUserId());
        facilityRepository.save(facility);
    }

    // ─── Mapping helpers ────────────────────────────────────────────

    private InvitationResponse toInvitationResponse(FacilityInvitation i) {
        return new InvitationResponse(
                i.getId(),
                i.getFacility().getId(),
                i.getEmail(),
                i.getRole(),
                i.getToken(),
                i.getInvitedByUserId(),
                i.getExpiresAt(),
                i.getAcceptedAt(),
                i.getRevokedAt(),
                i.getCreatedAt(),
                computeStatus(i)
        );
    }

    private String computeStatus(FacilityInvitation i) {
        if (i.getAcceptedAt() != null) return "ACCEPTED";
        if (i.getRevokedAt()  != null) return "REVOKED";
        if (i.getExpiresAt().isBefore(Instant.now())) return "EXPIRED";
        return "PENDING";
    }

    private MemberResponse toMemberResponse(FacilityMember m) {
        User user = m.getUser();
        return new MemberResponse(
                m.getFacility().getId(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                m.getRole(),
                m.getAddedByUserId(),
                m.getAddedAt()
        );
    }

    // ─── Internals ──────────────────────────────────────────────────

    private Facility loadFacility(Long id) {
        return facilityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
    }

    /** Generates a 32-char URL-safe token for invitations. */
    private String generateToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}








