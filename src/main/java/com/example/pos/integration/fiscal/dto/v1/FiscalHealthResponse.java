package com.example.pos.integration.fiscal.dto.v1;

public record FiscalHealthResponse(
        String status,
        String mode,
        String remoteUrl,
        Long latencyMs
) {}
