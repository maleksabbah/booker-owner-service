package com.malek.owner_service.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "facility_members")
@IdClass(FacilityMember.FacilityMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "added_by_user_id")
    private Long addedByUserId;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() {
        if (addedAt == null) addedAt = Instant.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilityMemberId implements Serializable {
        private Long facility;
        private Long user;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FacilityMemberId that)) return false;
            return Objects.equals(facility, that.facility) && Objects.equals(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(facility, user);
        }
    }
}
