package com.example.pos.pharmacy.regulatory.controlledrugs.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "controlled_drugs")
public class ControlledDrugs extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriptions_id")
    private Prescriptions prescriptions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer quantityDispensed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_items_id")
    private SaleItems saleItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}
