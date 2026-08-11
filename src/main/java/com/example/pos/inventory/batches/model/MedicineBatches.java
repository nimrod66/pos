package com.example.pos.inventory.batches.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.prescriptions.dispensary.model.Dispensary;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "medicine_batches", uniqueConstraints =
        @UniqueConstraint(name = "uk_batch_medicine_number", columnNames = {"medicine_id", "batch_number"}))
public class MedicineBatches extends BaseEntity {
    //link medicine_id, supplier_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Builder.Default
    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Stock> stock = new HashSet<>();

    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StockMovements> stockMovements;

    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SaleItems> saleItems;

    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PriceHistory> priceHistory;

    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SaleReturnItems> saleReturnItems;

    @Builder.Default
    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Dispensary> dispensary = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "medicineBatches", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ExpiryLogs> expiryLogs = new HashSet<>();

    @Column(nullable = false, length = 100)
    private String batchNumber;
    private LocalDate manufactureDate;
    private LocalDate expirationDate;
    private Integer initialQuantity;
    private BigDecimal buyingPrice;
    private BigDecimal sellingPrice;


}
