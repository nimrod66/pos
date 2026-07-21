package com.example.pos.compliance.synchronization.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "etims_sync_state")
public class EtimsSyncState extends BaseEntity {

    @Column(name = "sync_type", nullable = false, length = 30)
    private String syncType;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_sync_status", length = 20)
    private String lastSyncStatus;

    @Column(name = "records_synced")
    private int recordsSynced;

    @Column(name = "records_failed")
    private int recordsFailed;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "tenant_id")
    private Long tenantId;
}
