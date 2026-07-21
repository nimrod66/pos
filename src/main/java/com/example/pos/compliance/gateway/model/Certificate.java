package com.example.pos.compliance.gateway.model;

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
@Table(name = "compliance_certificates")
public class Certificate extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String serial;

    @Column(nullable = false, length = 200)
    private String issuer;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Column(name = "encrypted_private_key", columnDefinition = "LONGTEXT")
    private String encryptedPrivateKey;

    @Column(length = 64)
    private String thumbprint;

    @Column(name = "certificate_data", columnDefinition = "LONGTEXT")
    private String certificateData;

    @Column(name = "tenant_id")
    private Long tenantId;

    public enum CertificateStatus {
        ACTIVE, EXPIRED, REVOKED, PENDING
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == CertificateStatus.ACTIVE
                && (validFrom == null || !now.isBefore(validFrom))
                && (validTo == null || !now.isAfter(validTo));
    }
}
