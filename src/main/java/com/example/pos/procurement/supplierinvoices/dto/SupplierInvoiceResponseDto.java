package com.example.pos.procurement.supplierinvoices.dto;

import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
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
public class SupplierInvoiceResponseDto {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal balanceDue;
    private String status;
    private LocalDateTime createdAt;

    public static SupplierInvoiceResponseDto from(SupplierInvoices si) {
        return SupplierInvoiceResponseDto.builder()
                .id(si.getId()).supplierId(si.getSuppliers() != null ? si.getSuppliers().getId() : null)
                .supplierName(si.getSuppliers() != null ? si.getSuppliers().getSupplierName() : null)
                .invoiceNumber(si.getInvoiceNumber()).invoiceDate(si.getInvoiceDate())
                .subTotal(si.getSubTotal()).tax(si.getTax()).total(si.getTotal()).balanceDue(si.getBalanceDue())
                .status(si.getStatus() != null ? si.getStatus().name() : null).createdAt(si.getCreatedAt()).build();
    }
}
