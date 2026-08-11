package com.example.pos.procurement.goodsreceived.repository;

import java.util.UUID;

import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GoodsReceivedNotesRepository extends JpaRepository<GoodsReceivedNotes, UUID> {

    List<GoodsReceivedNotes> findByPurchaseOrdersId(UUID poId);

    Page<GoodsReceivedNotes> findByPurchaseOrdersIdAndBranchId(UUID poId, UUID branchId, Pageable pageable);

    Page<GoodsReceivedNotes> findByBranchId(UUID branchId, Pageable pageable);

    java.util.Optional<GoodsReceivedNotes> findByIdAndBranchId(UUID id, UUID branchId);
}
