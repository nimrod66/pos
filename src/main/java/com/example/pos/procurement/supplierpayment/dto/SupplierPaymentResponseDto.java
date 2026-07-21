package com.example.pos.procurement.supplierpayment.dto;

import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentResponseDto {

    private Long id;
    private Long supplierInvoiceId;
    private String invoiceNumber;
    private Long userId;
    private String userName;
    private String paymentMethod;
    private BigDecimal paymentAmount;
    private String paymentReference;
    private LocalDateTime paymentDate;

    public static SupplierPaymentResponseDto from(SupplierPayment sp) {
        return SupplierPaymentResponseDto.builder()
                .id(sp.getId())
                .supplierInvoiceId(sp.getSupplierInvoices() != null ? sp.getSupplierInvoices().getId() : null)
                .invoiceNumber(sp.getSupplierInvoices() != null ? sp.getSupplierInvoices().getInvoiceNumber() : null)
                .userId(sp.getUser() != null ? sp.getUser().getId() : null)
                .userName(sp.getUser() != null ? sp.getUser().getFirstName() : null)
                .paymentMethod(sp.getPaymentMethod()).paymentAmount(sp.getPaymentAmount())
                .paymentReference(sp.getPaymentReference()).paymentDate(sp.getPaymentDate()).build();
    }
}
