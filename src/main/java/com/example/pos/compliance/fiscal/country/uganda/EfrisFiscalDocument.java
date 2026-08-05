package com.example.pos.compliance.fiscal.country.uganda;

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
@Table(name = "efris_fiscal_documents")
public class EfrisFiscalDocument extends FiscalDocument {

    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "UG";

    @Column(name = "provider_code", nullable = false, length = 20)
    @Builder.Default
    private String providerCode = "EFRIS";

    @Column(name = "efris_invoice_number", length = 50)
    private String efrisInvoiceNumber;

    @Column(name = "tin", length = 20)
    private String tin;

    @Column(name = "buyer_tin", length = 20)
    private String buyerTin;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "branch_id")
    private UUID branchId;
}
