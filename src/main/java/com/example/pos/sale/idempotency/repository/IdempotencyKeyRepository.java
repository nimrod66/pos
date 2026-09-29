package com.example.pos.sale.idempotency.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.pos.sale.idempotency.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKey(String key);

    Optional<IdempotencyKey> findByPharmacyIdAndIdempotencyKey(UUID pharmacyId, String key);

    boolean existsByIdempotencyKey(String key);

    boolean existsByPharmacyIdAndIdempotencyKey(UUID pharmacyId, String key);

    List<IdempotencyKey> findByStatusAndCreatedAtBefore(IdempotencyKey.Status status,
                                                        LocalDateTime cutoff);

    /**
     * Sales keep a permanent foreign key to their checkout key, so keys that
     * are still referenced must never be deleted (the statement would abort
     * with a FK violation). Only unreferenced keys are purged.
     */
    @Modifying
    @Query("delete from IdempotencyKey key where key.status = :status and key.createdAt < :cutoff "
            + "and not exists (select sale.id from Sales sale where sale.idempotencyKey = key)")
    int deleteUnreferencedByStatusAndCreatedAtBefore(@Param("status") IdempotencyKey.Status status,
                                                     @Param("cutoff") LocalDateTime cutoff);
}
