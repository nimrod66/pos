package com.example.pos.compliance.tis;

import com.example.pos.compliance.gateway.model.ComplianceMode;

public record TisWorkflow(
        boolean autoInvoice,
        boolean autoTransmit,
        boolean strictValidation,
        ComplianceMode mode
) {}
