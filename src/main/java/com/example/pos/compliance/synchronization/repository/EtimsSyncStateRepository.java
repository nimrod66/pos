package com.example.pos.compliance.synchronization.repository;

import com.example.pos.compliance.synchronization.model.EtimsSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtimsSyncStateRepository extends JpaRepository<EtimsSyncState, Long> {

    Optional<EtimsSyncState> findBySyncTypeAndTenantId(String syncType, Long tenantId);
}
