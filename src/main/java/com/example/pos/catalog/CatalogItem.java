package com.example.pos.catalog;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "supplier_catalog_items")
public class CatalogItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private Catalog catalog;

    @Column(name = "supplier_code", length = 50, nullable = false)
    private String supplierCode;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "generic_name", length = 200)
    private String genericName;

    @Column(name = "dosage_form", length = 50)
    private String dosageForm;

    @Column(length = 50)
    private String strength;

    @Column(name = "pack_size", length = 20)
    private String packSize;

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    @Column(name = "manufacturer_name", length = 150)
    private String manufacturerName;

    @Column(name = "manufacturer_country", length = 50)
    private String manufacturerCountry;

    @Column(name = "etims_classification_code", length = 50)
    private String etimsClassificationCode;

    @Column(length = 50)
    private String barcode;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "atc_code", length = 20)
    private String atcCode;

    @Column(name = "matched_medicine_id")
    private UUID matchedMedicineId;

    @Column(name = "match_confidence", length = 10)
    private String matchConfidence;

    public enum MatchConfidence {
        HIGH, MEDIUM, LOW, NONE
    }
}
