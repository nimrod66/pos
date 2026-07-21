package com.example.pos.compliance.etims.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.sales.model.Sales;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "etims_logs")
public class Etims extends BaseEntity {
    //link sale id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id")
    private Sales sales;

    private String submissionStatus;
    private String qrCode;
}
