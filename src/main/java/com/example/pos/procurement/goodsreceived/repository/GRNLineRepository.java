package com.example.pos.procurement.goodsreceived.repository;

import com.example.pos.procurement.goodsreceived.model.GRNLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GRNLineRepository extends JpaRepository<GRNLine, UUID> {

    @Query("select coalesce(sum(line.quantity), 0) from GRNLine line where line.purchaseOrderLineId = :lineId")
    long sumQuantityByPurchaseOrderLineId(@Param("lineId") UUID lineId);

    @Query("select line from GRNLine line " +
            "join line.goodsReceivedNotes grn " +
            "join grn.supplier supplier " +
            "join line.medicine medicine " +
            "where medicine.pharmacy.id = :pharmacyId " +
            "order by medicine.brandName, supplier.supplierName")
    List<GRNLine> findAllByPharmacyId(@Param("pharmacyId") UUID pharmacyId);
}
