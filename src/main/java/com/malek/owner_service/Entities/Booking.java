package com.malek.owner_service.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Column(nullable = false, length = 20)
    private String visibility;   // 'PRIVATE' | 'PUBLIC'

    @Column(nullable = false, length = 20)
    private String state;        // 'HELD' | 'CONFIRMED' | 'SEATED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

    @Column(name = "slots_total", nullable = false)
    private Integer slotsTotal;

    @Column(name = "slots_filled", nullable = false)
    private Integer slotsFilled;

    @Column(name = "min_players", nullable = false)
    private Integer minPlayers;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_total")
    private BigDecimal priceTotal;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── NEW (migration 014) — audit who triggered each terminal/intermediate state
    @Column(name = "seated_by_user_id")
    private Long seatedByUserId;

    @Column(name = "completed_by_user_id")
    private Long completedByUserId;

    @Column(name = "no_show_by_user_id")
    private Long noShowByUserId;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}