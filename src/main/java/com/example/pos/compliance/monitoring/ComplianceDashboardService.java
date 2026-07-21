package com.example.pos.compliance.monitoring;

import com.example.pos.compliance.config.ComplianceConfiguration;
import com.example.pos.compliance.dashboard.dto.ComplianceDashboardDto;
import com.example.pos.compliance.gateway.ComplianceGatewayFactory;
import com.example.pos.compliance.initialization.EtimsInitializer;
import com.example.pos.compliance.synchronization.model.EtimsSyncState;
import com.example.pos.compliance.synchronization.repository.EtimsSyncStateRepository;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import com.example.pos.compliance.transmission.repository.DeadLetterRecordRepository;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import com.example.pos.compliance.transmission.service.InMemoryTransmissionQueue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplianceDashboardService {

    private final TransmissionRepository transmissionRepo;
    private final DeadLetterRecordRepository deadLetterRepo;
    private final ComplianceGatewayFactory gatewayFactory;
    private final ComplianceConfiguration config;
    private final InMemoryTransmissionQueue queue;
    private final EtimsSyncStateRepository syncStateRepo;

    public ComplianceDashboardService(TransmissionRepository transmissionRepo,
                                      DeadLetterRecordRepository deadLetterRepo,
                                      ComplianceGatewayFactory gatewayFactory,
                                      ComplianceConfiguration config,
                                      InMemoryTransmissionQueue queue,
                                      EtimsSyncStateRepository syncStateRepo) {
        this.transmissionRepo = transmissionRepo;
        this.deadLetterRepo = deadLetterRepo;
        this.gatewayFactory = gatewayFactory;
        this.config = config;
        this.queue = queue;
        this.syncStateRepo = syncStateRepo;
    }

    public ComplianceDashboardDto getDashboard() {
        long pending = transmissionRepo.countByTransmissionStatus(TransmissionStatus.PENDING);
        long failed = transmissionRepo.countByTransmissionStatus(TransmissionStatus.FAILED);
        long transmitted = transmissionRepo.countByTransmissionStatus(TransmissionStatus.TRANSMITTED);
        long deadCount = deadLetterRepo.countByStatus(
                com.example.pos.compliance.transmission.model.DeadLetterRecord.DeadLetterStatus.PENDING);

        String oscuStatus = getHealthString("OSCU");
        String vscuStatus = gatewayFactory.hasProvider("VSCU")
                ? getHealthString("VSCU") : "NOT_CONFIGURED";

        List<EtimsSyncState> syncStates = syncStateRepo.findAll();

        ComplianceDashboardDto dto = new ComplianceDashboardDto();
        dto.setMode(config.getMode().name());
        dto.setActiveProvider(config.getActiveProvider());
        dto.setInvoicesToday(0);
        dto.setTransmissionsPending(pending);
        dto.setTransmissionsFailed(failed);
        dto.setTransmissionsTransmitted(transmitted);
        dto.setDeadLetterCount(deadCount);
        dto.setRetryQueueSize(queue.size());
        dto.setOscuStatus(oscuStatus);
        dto.setVscuStatus(vscuStatus);
        dto.setCertificateStatus("ACTIVE");
        dto.setCertificateExpiring(false);
        dto.setLastSuccess(null);
        dto.setLastFailure(null);
        dto.setAverageApiTimeMs("N/A");

        return dto;
    }

    public List<EtimsSyncState> getSyncStatus() {
        return syncStateRepo.findAll();
    }

    private String getHealthString(String provider) {
        try {
            return gatewayFactory.getGateway(provider).getHealth();
        } catch (Exception e) {
            return "UNREACHABLE";
        }
    }
}
