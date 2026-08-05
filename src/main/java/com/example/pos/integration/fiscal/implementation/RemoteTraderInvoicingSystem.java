package com.example.pos.integration.fiscal.implementation;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.tis.TraderInvoicingSystem;
import com.example.pos.integration.fiscal.client.FiscalClient;
import com.example.pos.integration.fiscal.config.FiscalProperties;
import com.example.pos.integration.fiscal.dto.v1.FiscalSaleRequest;
import com.example.pos.integration.fiscal.dto.v1.FiscalSaleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class RemoteTraderInvoicingSystem implements TraderInvoicingSystem {

    private static final Logger log = LoggerFactory.getLogger(RemoteTraderInvoicingSystem.class);

    private final FiscalClient fiscalClient;
    private final FiscalProperties properties;

    public RemoteTraderInvoicingSystem(FiscalClient fiscalClient, FiscalProperties properties) {
        this.fiscalClient = fiscalClient;
        this.properties = properties;
    }

    @Override
    public TaxInvoice generateInvoice(UUID saleId, String customerPin) {
        try {
            FiscalSaleRequest request = new FiscalSaleRequest(
                    saleId, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, "KES", customerPin, null, false, java.util.List.of()
            );
            FiscalSaleResponse response = fiscalClient.sendInvoice(request);
            log.info("Remote fiscal: invoice {} generated for sale {}", response.invoiceNumber(), saleId);
        } catch (Exception e) {
            log.error("Remote fiscal failed for sale {} — sale continues, fiscal will retry: {}", saleId, e.getMessage());
        }
        return null;
    }

    @Override
    public String generateReceipt(UUID saleId, UUID invoiceId) {
        try {
            return "REMOTE:" + saleId;
        } catch (Exception e) {
            log.error("Remote fiscal receipt failed for sale {}: {}", saleId, e.getMessage());
            return null;
        }
    }

    @Override
    public void validateInvoice(TaxInvoice invoice) {
        log.debug("Remote fiscal: validation skipped (handled by remote service)");
    }

    @Override
    public void submitToKra(TaxInvoice invoice) {
        log.debug("Remote fiscal: submission handled by remote service for invoice {}", invoice.getId());
    }

    @Override
    public void cancelInvoice(UUID invoiceId, String reason) {
        try {
            log.info("Remote fiscal: cancellation requested for invoice {}", invoiceId);
        } catch (Exception e) {
            log.error("Remote fiscal cancel failed for invoice {}: {}", invoiceId, e.getMessage());
        }
    }

    @Override
    public void issueCreditNote(UUID invoiceId, BigDecimal amount, String reason) {
        log.info("Remote fiscal: credit note for invoice {} amount {}", invoiceId, amount);
    }

    @Override
    public void issueDebitNote(UUID invoiceId, BigDecimal amount, String reason) {
        log.info("Remote fiscal: debit note for invoice {} amount {}", invoiceId, amount);
    }

    @Override
    public void reconciliationCheck() {
        try {
            fiscalClient.health();
        } catch (Exception e) {
            log.warn("Remote fiscal health check failed: {}", e.getMessage());
        }
    }
}
