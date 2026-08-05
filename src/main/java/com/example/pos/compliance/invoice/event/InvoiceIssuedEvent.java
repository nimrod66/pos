package com.example.pos.compliance.invoice.event;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class InvoiceIssuedEvent extends ApplicationEvent {

    private final UUID invoiceId;
    private final UUID saleId;
    private final String invoiceNumber;

    public InvoiceIssuedEvent(Object source, TaxInvoice invoice) {
        super(source);
        this.invoiceId = invoice.getId();
        this.saleId = invoice.getSaleId();
        this.invoiceNumber = invoice.getInvoiceNumber();
    }

    public UUID getInvoiceId() { return invoiceId; }
    public UUID getSaleId() { return saleId; }
    public String getInvoiceNumber() { return invoiceNumber; }
}
