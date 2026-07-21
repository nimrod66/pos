package com.example.pos.procurement.pricehistory.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "price_history")
public class PriceHistory extends BaseEntity {
    //link medicine id, link batch id, link users

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal oldBuyingPrice;
    private BigDecimal oldSellingPrice;
    private BigDecimal newBuyingPrice;
    private BigDecimal newSellingPrice;
    private LocalDateTime changedAt;
}
