package com.example.pos.compliance.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceDashboardDto {

    private String mode;
    private String activeProvider;

    private long invoicesToday;
    private long transmissionsPending;
    private long transmissionsFailed;
    private long transmissionsTransmitted;
    private long deadLetterCount;
    private long retryQueueSize;

    private String oscuStatus;
    private String vscuStatus;
    private String certificateStatus;
    private boolean certificateExpiring;

    private LocalDateTime lastSuccess;
    private LocalDateTime lastFailure;

    private String averageApiTimeMs;
}
