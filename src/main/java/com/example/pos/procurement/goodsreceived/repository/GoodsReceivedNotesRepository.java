package com.example.pos.procurement.goodsreceived.repository;

import java.util.UUID;

import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceivedNotesRepository extends JpaRepository<GoodsReceivedNotes, UUID> {

    List<GoodsReceivedNotes> findByPurchaseOrdersId(UUID poId);
}
