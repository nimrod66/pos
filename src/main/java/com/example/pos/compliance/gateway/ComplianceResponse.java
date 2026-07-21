package com.example.pos.compliance.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceResponse {

    private boolean success;
    private String statusCode;
    private String receiptNumber;
    private String message;
    private String rawResponse;
    private long durationMs;
}
