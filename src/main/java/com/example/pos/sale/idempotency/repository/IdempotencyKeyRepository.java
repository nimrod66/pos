package com.example.pos.sale.idempotency.repository;

import com.example.pos.sale.idempotency.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByIdempotencyKey(String key);

    boolean existsByIdempotencyKey(String key);
}
