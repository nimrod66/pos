package com.example.pos.compliance.invoice.event;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import org.springframework.context.ApplicationEvent;

public class InvoiceIssuedEvent extends ApplicationEvent {

    private final Long invoiceId;
    private final Long saleId;
    private final String invoiceNumber;

    public InvoiceIssuedEvent(Object source, TaxInvoice invoice) {
        super(source);
        this.invoiceId = invoice.getId();
        this.saleId = invoice.getSaleId();
        this.invoiceNumber = invoice.getInvoiceNumber();
    }

    public Long getInvoiceId() { return invoiceId; }
    public Long getSaleId() { return saleId; }
    public String getInvoiceNumber() { return invoiceNumber; }
}
