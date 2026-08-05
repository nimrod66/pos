package com.example.pos.compliance.sync;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SyncStateRepository extends JpaRepository<SyncState, UUID> {
    Optional<SyncState> findBySyncTypeAndTenantId(String syncType, UUID tenantId);
}