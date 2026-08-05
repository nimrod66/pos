package com.example.pos.compliance.config.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "tax_periods")
public class TaxPeriod extends BaseEntity {

    @Column(name = "period_code", nullable = false, length = 20)
    private String periodCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDate submissionDeadline;

    @Column(name = "is_submitted")
    @Builder.Default
    private boolean isSubmitted = false;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
