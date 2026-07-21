package com.example.pos.compliance.tis;

public interface TisExporter {
    String preparePayload(Long invoiceId);
    void markExported(Long invoiceId);
}
