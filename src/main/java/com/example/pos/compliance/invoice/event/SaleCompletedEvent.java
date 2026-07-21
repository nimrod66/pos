package com.example.pos.compliance.invoice.event;

import com.example.pos.sale.sales.model.Sales;
import org.springframework.context.ApplicationEvent;

public class SaleCompletedEvent extends ApplicationEvent {

    private final Long saleId;
    private final Long branchId;
    private final String invoiceNumber;

    public SaleCompletedEvent(Object source, Sales sale) {
        super(source);
        this.saleId = sale.getId();
        this.branchId = sale.getBranch() != null ? sale.getBranch().getId() : null;
        this.invoiceNumber = sale.getInvoiceNumber();
    }

    public Long getSaleId() { return saleId; }
    public Long getBranchId() { return branchId; }
    public String getInvoiceNumber() { return invoiceNumber; }
}
