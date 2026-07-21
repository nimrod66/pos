package com.example.pos.compliance.gateway;

import com.example.pos.compliance.invoice.model.TaxInvoice;

public interface ComplianceGateway {

    String getProviderName();

    ComplianceResponse submit(TaxInvoice invoice, String payload);

    ComplianceResponse queryStatus(String invoiceNumber);

    String getHealth();

    boolean supports(String providerCode);
}
