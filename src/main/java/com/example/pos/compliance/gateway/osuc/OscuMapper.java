package com.example.pos.compliance.gateway.osuc;

import com.example.pos.compliance.gateway.model.OscuPayload;
import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.TaxInvoiceItem;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OscuMapper {

    public OscuPayload toPayload(TaxInvoice invoice) {
        List<OscuPayload.OscuPayloadItem> items = invoice.getItems().stream()
                .map(this::toItem).toList();

        return OscuPayload.builder()
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getIssueDate() != null
                        ? invoice.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .currency(invoice.getCurrency())
                .customerPin(invoice.getCustomerPin())
                .customerName(invoice.getCustomerName())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .discount(invoice.getDiscount())
                .grandTotal(invoice.getGrandTotal())
                .items(items)
                .build();
    }

    public String extractReceiptNumber(String response) {
        return null;
    }

    private OscuPayload.OscuPayloadItem toItem(TaxInvoiceItem item) {
        return OscuPayload.OscuPayloadItem.builder()
                .itemCode(item.getBarcode())
                .itemBarcodeType(item.getBarcodeType())
                .itemClassificationCode(item.getEtimsClassificationCode())
                .itemName(item.getMedicineName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxableAmount(item.getTaxableAmount())
                .taxRate(item.getTaxRate())
                .taxAmount(item.getTaxAmount())
                .total(item.getTotal())
                .build();
    }
}
