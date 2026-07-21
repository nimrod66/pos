package com.example.pos.sync.model;

import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "terminals")
public class Terminal {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String terminalId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 128)
    private String apiKey;

    @Column(nullable = false, length = 16)
    private String apiSecret;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    private LocalDateTime registeredAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean synced = false;

    @Column(length = 36)
    private String branchId;

    @PrePersist
    public void onRegister() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }

    public static String generateApiKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateApiSecret() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
