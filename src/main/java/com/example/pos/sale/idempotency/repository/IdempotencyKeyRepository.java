package com.example.pos.sale.idempotency.repository;

import java.util.UUID;

import com.example.pos.sale.idempotency.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKey(String key);

    Optional<IdempotencyKey> findByPharmacyIdAndIdempotencyKey(UUID pharmacyId, String key);

    boolean existsByIdempotencyKey(String key);

    boolean existsByPharmacyIdAndIdempotencyKey(UUID pharmacyId, String key);

    long deleteByStatusAndCreatedAtBefore(IdempotencyKey.Status status, java.time.LocalDateTime cutoff);
}
