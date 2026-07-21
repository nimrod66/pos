package com.example.pos.procurement.goodsreceived.repository;

import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceivedNotesRepository extends JpaRepository<GoodsReceivedNotes, Long> {

    List<GoodsReceivedNotes> findByPurchaseOrdersId(Long poId);
}
