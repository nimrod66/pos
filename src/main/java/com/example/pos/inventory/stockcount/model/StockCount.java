package com.example.pos.inventory.stockcount.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "stock_counts")
public class StockCount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counted_by_id", nullable = false)
    private User countedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private String remarks;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockCountItem> items = new ArrayList<>();

    public enum Status { DRAFT, COUNTED, RECONCILED }
}
