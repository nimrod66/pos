package com.example.pos.terminal.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Entity(name = "TerminalRegistry")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "terminal_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_terminal_id", columnNames = "terminal_id"),
        @UniqueConstraint(name = "uk_terminal_branch_name", columnNames = {"branch_id", "name"}),
        @UniqueConstraint(name = "uk_terminal_api_key", columnNames = "api_key")
})
public class Terminal extends BaseEntity {

    @Column(name = "terminal_id", length = 36, nullable = false, unique = true)
    @Builder.Default
    private String terminalId = "T-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "terminal_type", nullable = false, length = 30)
    private TerminalType terminalType;

    @Column(length = 50)
    private String manufacturer;

    @Column(length = 50)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(length = 30)
    private String platform;

    @Column(name = "os_version", length = 30)
    private String osVersion;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "api_key", nullable = false, length = 128)
    private String apiKey;

    @Column(name = "api_secret", nullable = false, length = 128)
    private String apiSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TerminalStatus status = TerminalStatus.PENDING;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "registered_by", length = 50)
    private String registeredBy;

    @Column(name = "registered_at")
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "supported_api_version", length = 30)
    private String supportedApiVersion;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "minimum_backend_version", length = 30)
    private String minimumBackendVersion;

    @Column(name = "migrated_from_terminal")
    @Builder.Default
    private boolean migratedFromTerminal = false;

    @PrePersist
    public void onRegister() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }

    public static String generateApiKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }

    public static String generateApiSecret() {
        byte[] secret = new byte[16];
        new SecureRandom().nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }
}
