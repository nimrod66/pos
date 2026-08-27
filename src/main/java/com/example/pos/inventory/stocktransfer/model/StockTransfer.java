package com.example.pos.inventory.stocktransfer.model;

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
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_branch_id", nullable = false)
    private Branch sourceBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dest_branch_id", nullable = false)
    private Branch destBranch;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_id")
    private User receivedBy;

    private LocalDate transferDate;
    private LocalDate receivedDate;
    private String remarks;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockTransferItem> items = new ArrayList<>();

    public enum Status { PENDING, APPROVED, IN_TRANSIT, RECEIVED, CANCELLED }
}
