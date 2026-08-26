package com.example.pos.masterdata.medicine.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.terminal.barcode.BarcodeSource;
import com.example.pos.terminal.barcode.BarcodeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "medicine", uniqueConstraints =
        @UniqueConstraint(name = "uk_medicine_pharmacy_barcode", columnNames = {"pharmacy_id", "barcode"}))
public class Medicine extends BaseEntity {
    //link manufacturer id, category id, dosage form id, unit id, tax category id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_categories_id")
    private MedicineCategories medicineCategories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dosage_form_id")
    private DosageForm dosageForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buying_unit_id")
    private Unit buyingUnit;

    @Column(name = "pack_size")
    private Integer packSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_id")
    private Tax tax;

    @Builder.Default
    @OneToMany(mappedBy = "medicine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<MedicineBatches> medicineBatches = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "medicine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PurchaseOrderItems> purchaseOrderItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "medicine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PriceHistory> priceHistory = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "medicine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PrescriptionItems> prescriptionItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "medicine", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ControlledDrugs> controlledDrugs = new HashSet<>();

    private String barcode;

    @Column(name = "manufacturer_barcode", length = 50)
    private String manufacturerBarcode;

    @Column(name = "internal_barcode", length = 50)
    private String internalBarcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", length = 20)
    private BarcodeType barcodeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_source", length = 20)
    private BarcodeSource barcodeSource;

    @Column(name = "kemsa_code", length = 50)
    private String kemsaCode;

    @Column(name = "ppb_code", length = 50)
    private String ppbCode;

    @Column(name = "etims_item_code", length = 50)
    private String etimsItemCode;

    @Column(name = "gs1_company_prefix", length = 20)
    private String gs1CompanyPrefix;

    @Column(name = "track_serial_number")
    private boolean trackSerialNumber;

    @Column(name = "track_batch")
    @Builder.Default
    private boolean trackBatch = true;

    @Column(name = "track_expiry")
    @Builder.Default
    private boolean trackExpiry = true;

    private String sku;
    private String brandName;
    private String genericName;
    private String strength;
    @Column(name = "default_buying_price", nullable = false)
    private BigDecimal buyingPrice;
    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        AVAILABLE, NOT_AVAILABLE
    }

    private boolean requiresPrescription;
    private String description;
    private Integer maximumDispenseQuantity;
    private String minimumAge;
    private boolean requiresRefrigeration;
    private boolean isControlledDrug;

}
