package com.example.pos.compliance.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
    Optional<SyncState> findBySyncTypeAndTenantId(String syncType, Long tenantId);
}