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
@Table(name = "fiscal_years")
public class FiscalYear extends BaseEntity {

    @Column(name = "year_code", unique = true, nullable = false, length = 20)
    private String yearCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private boolean isCurrent = false;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
