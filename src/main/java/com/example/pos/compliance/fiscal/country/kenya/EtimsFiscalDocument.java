package com.example.pos.compliance.fiscal.country.kenya;

import com.example.pos.compliance.invoice.model.FiscalDocument;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "etims_fiscal_documents")
public class EtimsFiscalDocument extends FiscalDocument {

    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "KE";

    @Column(name = "provider_code", nullable = false, length = 20)
    @Builder.Default
    private String providerCode = "eTIMS";

    @Column(name = "kra_pin", length = 20)
    private String kraPin;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "receipt_code", length = 50)
    private String receiptCode;

    @Column(name = "customer_pin", length = 20)
    private String customerPin;

    @Column(name = "supplier_pin", length = 20)
    private String supplierPin;

    @Column(name = "control_unit_serial", length = 50)
    private String controlUnitSerial;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "transmission_id")
    private UUID transmissionId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "branch_id")
    private UUID branchId;
}
