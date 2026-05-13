package com.malek.owner_service.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.malek.owner_service.Entities.IdempotencyRecord;
import com.malek.owner_service.Repositories.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Wrap any operation in idempotency logic.
     * - First call: runs the operation, stores the response, returns it.
     * - Replay (same key + same request): returns the stored response without re-running.
     * - Same key but different request payload: 422 (mismatch).
     */
    @Transactional
    public <T> T executeIdempotent(
            String key,
            Long userId,
            String endpoint,
            Object requestPayload,
            Class<T> responseType,
            Supplier<T> operation) {

        String requestHash = hash(requestPayload);

        Optional<IdempotencyRecord> existing =
                repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint);

        if (existing.isPresent()) {
            IdempotencyRecord rec = existing.get();
            if (!rec.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Idempotency key reused with a different request payload");
            }
            return deserialize(rec.getResponseBody(), responseType);
        }

        T result = operation.get();

        IdempotencyRecord rec = IdempotencyRecord.builder()
                .key(key)
                .userId(userId)
                .endpoint(endpoint)
                .requestHash(requestHash)
                .responseStatus(200)
                .responseBody(serialize(result))
                .expiresAt(Instant.now().plus(TTL))
                .build();

        try {
            repository.save(rec);
        } catch (DataIntegrityViolationException e) {
            // Race: another request inserted with the same key while we ran.
            // Re-read and replay.
            IdempotencyRecord raced = repository.findByKeyAndUserIdAndEndpoint(key, userId, endpoint)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Idempotency race recovery failed"));
            return deserialize(raced.getResponseBody(), responseType);


        }
        return result;
    }

    // ─── helpers ────────────────────────────────────────────────────
    private String hash(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hashing failed", e);
        }
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed", e);

        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Deserialization failed", e);

        }

    }

}


