package com.example.pos.compliance.monitoring.reconciliation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationResult {

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long localInvoiceCount;
    private long kraAcknowledgedCount;
    private long missingAtKra;
    private long orphanedAtKra;
    private List<String> missingDocuments;
    private boolean success;
    private String error;
}
