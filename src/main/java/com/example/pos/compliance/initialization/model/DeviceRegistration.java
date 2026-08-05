package com.example.pos.compliance.initialization.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "device_registration")
public class DeviceRegistration extends BaseEntity {

    @Column(name = "device_serial", unique = true, nullable = false, length = 50)
    private String deviceSerial;

    @Column(name = "kra_pin", nullable = false, length = 20)
    private String kraPin;

    @Column(name = "encrypted_cmc_key", nullable = false, columnDefinition = "LONGTEXT")
    private String encryptedCmcKey;

    @Column(name = "registration_status", nullable = false, length = 20)
    @Builder.Default
    private String registrationStatus = "PENDING";

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "last_renewed_at")
    private LocalDateTime lastRenewedAt;

    @Column(name = "environment", nullable = false, length = 20)
    @Builder.Default
    private String environment = "SANDBOX";

    @Column(name = "tenant_id")
    private UUID tenantId;

    public enum RegistrationStatus {
        PENDING, INITIALIZED, ACTIVE, EXPIRED, REVOKED
    }
}
