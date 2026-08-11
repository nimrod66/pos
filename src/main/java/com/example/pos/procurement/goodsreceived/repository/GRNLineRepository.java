package com.example.pos.procurement.goodsreceived.repository;

import com.example.pos.procurement.goodsreceived.model.GRNLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface GRNLineRepository extends JpaRepository<GRNLine, UUID> {

    @Query("select coalesce(sum(line.quantity), 0) from GRNLine line where line.purchaseOrderLineId = :lineId")
    long sumQuantityByPurchaseOrderLineId(@Param("lineId") UUID lineId);
}
