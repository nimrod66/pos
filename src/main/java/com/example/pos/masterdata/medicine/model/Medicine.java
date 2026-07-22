package com.example.pos.masterdata.medicine.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.presciptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "medicine")
public class Medicine extends BaseEntity {
    //link manufacturer id, category id, dosage form id, unit id, tax category id

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
    private String sku;
    private String brandName;
    private String genericName;
    private String strength;
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
