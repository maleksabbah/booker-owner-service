package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.FacilityDto.FacilityResponse;
import com.malek.owner_service.Dtos.FacilityDto.FacilityRequest;
import com.malek.owner_service.Entities.Facility;
import com.malek.owner_service.Entities.FacilityMember;
import com.malek.owner_service.Entities.User;
import com.malek.owner_service.GlobalExceptionHandler;
import com.malek.owner_service.Repositories.FacilityMemberRepository;
import com.malek.owner_service.Repositories.FacilityRepository;
import com.malek.owner_service.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
public class FacilityService {
    private final FacilityRepository facilityRepository;
    private final FacilityMemberRepository facilityMemberRepository;
    private final UserRepository userRepository;

    private static int rank(String role) {
        return switch (role) {
            case "STAFF" -> 1;
            case "MANAGER" -> 2;
            case "OWNER" -> 3;
            default -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        };

    }
    @Transactional
    public FacilityResponse createFacility(FacilityRequest req, Long callerUserId) {
        User caller = userRepository.findById(callerUserId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"User not found"));
        Facility facility = Facility.builder()
                .ownerUserId(callerUserId)
                .name(req.name())
                .description(req.description())
                .address(req.address())
                .city(req.city())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .phone(req.phone())
                .email(req.email())
                .website(req.website())
                .opensAt(req.opensAt())
                .closesAt(req.closesAt())
                .active(true)
                .build();
        facility = facilityRepository.save(facility);

        // Auto-add creator as the OWNER member
        FacilityMember membership = FacilityMember.builder()
                .facility(facility)
                .user(caller)
                .role("OWNER")
                .addedByUserId(null)            // null = system-generated initial owner
                .addedAt(Instant.now())
                .build();
        facilityMemberRepository.save(membership);

        return toResponse(facility);
    }
    public FacilityResponse getFacility(Long id, Long callerUserId) {
        requireRole(callerUserId,id, "STAFF");
        Facility facility = loadActive(id);
        return toResponse(facility);
    }
    @Transactional(readOnly = true)
    public List<FacilityResponse> listMyFacilities(Long callerUserId) {
        return facilityMemberRepository.findByUser_Id(callerUserId).stream()
                .map(FacilityMember::getFacility)
                .filter(Facility::getActive)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FacilityResponse updateFacility(Long id,FacilityRequest req, Long callerUserId) {
        requireRole(callerUserId,id,"MANAGER");
        Facility f = loadActive(id);

        // PATCH semantics — only overwrite fields that were provided
        if (req.name()        != null) f.setName(req.name());
        if (req.description() != null) f.setDescription(req.description());
        if (req.address()     != null) f.setAddress(req.address());
        if (req.city()        != null) f.setCity(req.city());
        if (req.latitude()    != null) f.setLatitude(req.latitude());
        if (req.longitude()   != null) f.setLongitude(req.longitude());
        if (req.phone()       != null) f.setPhone(req.phone());
        if (req.email()       != null) f.setEmail(req.email());
        if (req.website()     != null) f.setWebsite(req.website());
        if (req.opensAt()     != null) f.setOpensAt(req.opensAt());
        if (req.closesAt()    != null) f.setClosesAt(req.closesAt());

        return toResponse(facilityRepository.save(f));


    }
    @Transactional
    public void softDeleteFacility(Long id,Long callerUserId) {
        requireRole(callerUserId,id,"OWNER");
        Facility f = loadActive(id);
        f.setActive(false);
        facilityRepository.save(f);
    }

    // ─── Permission helper used by all owner-service services ──────

    /**
     * Throws 403 if the caller isn't a member of the facility, or has a role
     * below the required minimum. Other services call this before any mutation.
     */
    public void requireRole(Long callerUserId, Long facilityId, String minimumRole) {
        FacilityMember membership = facilityMemberRepository
                .findByFacility_IdAndUser_Id(facilityId, callerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this facility"));

        if (rank(membership.getRole()) < rank(minimumRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Requires role" + minimumRole + "or higher; you are" + membership.getRole());
        }
    }

        // ─── Internals ──────────────────────────────────────────────────


    public Facility loadActive (Long id){
        return facilityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Facility not found"));
    }

    private FacilityResponse toResponse (Facility f) {
            return new FacilityResponse(
                    f.getId(),
                    f.getOwnerUserId(),
                    f.getName(),
                    f.getDescription(),
                    f.getAddress(),
                    f.getCity(),
                    f.getLatitude(),
                    f.getLongitude(),
                    f.getPhone(),
                    f.getEmail(),
                    f.getWebsite(),
                    f.getOpensAt(),
                    f.getClosesAt(),
                    f.getActive(),
                    f.getCreatedAt(),
                    f.getUpdatedAt()
            );
    }


    }






