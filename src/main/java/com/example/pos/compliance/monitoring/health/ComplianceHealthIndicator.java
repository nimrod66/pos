package com.example.pos.compliance.monitoring.health;

import com.example.pos.compliance.config.ComplianceConfiguration;
import com.example.pos.compliance.gateway.ComplianceGatewayFactory;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import com.example.pos.compliance.transmission.repository.DeadLetterRecordRepository;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import com.example.pos.compliance.gateway.model.Certificate;
import com.example.pos.compliance.gateway.repository.CertificateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ComplianceHealthIndicator {

    private final ComplianceGatewayFactory gatewayFactory;
    private final TransmissionRepository transmissionRepo;
    private final DeadLetterRecordRepository deadLetterRepo;
    private final CertificateRepository certificateRepo;
    private final ComplianceConfiguration config;

    public ComplianceHealthIndicator(ComplianceGatewayFactory gatewayFactory,
                                     TransmissionRepository transmissionRepo,
                                     DeadLetterRecordRepository deadLetterRepo,
                                     CertificateRepository certificateRepo,
                                     ComplianceConfiguration config) {
        this.gatewayFactory = gatewayFactory;
        this.transmissionRepo = transmissionRepo;
        this.deadLetterRepo = deadLetterRepo;
        this.certificateRepo = certificateRepo;
        this.config = config;
    }

    public Map<String, Object> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mode", config.getMode().name());
        details.put("activeProvider", config.getActiveProvider());
        details.put("status", "UP");

        try {
            details.put("transmissionsPending",
                    transmissionRepo.countByTransmissionStatus(TransmissionStatus.PENDING));
            details.put("transmissionsFailed",
                    transmissionRepo.countByTransmissionStatus(TransmissionStatus.FAILED));
            details.put("transmissionsTransmitted",
                    transmissionRepo.countByTransmissionStatus(TransmissionStatus.TRANSMITTED));
            details.put("deadLetterCount",
                    deadLetterRepo.countByStatus(
                            com.example.pos.compliance.transmission.model.DeadLetterRecord.DeadLetterStatus.PENDING));
        } catch (Exception e) {
            details.put("transmissionStats", "ERROR: " + e.getMessage());
        }

        try {
            details.put("oscuHealth", gatewayFactory.getGateway("OSCU").getHealth());
        } catch (Exception e) {
            details.put("oscuHealth", "UNREACHABLE");
            details.put("status", "DOWN");
        }

        try {
            var certs = certificateRepo.findByTenantIdAndStatus(null, Certificate.CertificateStatus.ACTIVE);
            details.put("activeCertificates", certs.size());
            certs.stream()
                    .filter(c -> c.getValidTo() != null
                            && c.getValidTo().isBefore(LocalDateTime.now().plusDays(30)))
                    .findAny()
                    .ifPresent(c -> details.put("certificateWarning", "Certificate expiring within 30 days"));
        } catch (Exception e) {
            details.put("certificates", "ERROR");
        }

        return details;
    }
}
