package com.example.pos.compliance.gateway;

import com.example.pos.compliance.invoice.model.TaxInvoice;

public interface FiscalDevice {

    String getDeviceId();

    String getDeviceType();

    ComplianceResponse submit(TaxInvoice invoice, String payload);

    ComplianceResponse submitBatch(String batchRef, java.util.List<String> payloads);

    ComplianceResponse queryStatus(String invoiceNumber);

    String getHealth();
}
