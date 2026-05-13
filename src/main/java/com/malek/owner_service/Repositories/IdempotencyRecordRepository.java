package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByKeyAndUserIdAndEndpoint(String key, Long userId, String endpoint);
    void deleteByExpiresAtBefore(Instant cutoff);
}
