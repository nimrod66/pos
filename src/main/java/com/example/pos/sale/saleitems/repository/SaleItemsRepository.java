package com.example.pos.sale.saleitems.repository;

import java.util.UUID;

import com.example.pos.sale.saleitems.model.SaleItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface SaleItemsRepository extends JpaRepository<SaleItems, UUID> {

    @Query("select coalesce(sum(item.quantity), 0) from SaleReturnItems item "
            + "where item.saleItems.id = :saleItemId and item.saleReturns.status = 'COMPLETED'")
    int sumCompletedReturnQuantity(@Param("saleItemId") UUID saleItemId);

    @Query("select coalesce(sum(item.refundAmount), 0) from SaleReturnItems item "
            + "where item.saleItems.id = :saleItemId and item.saleReturns.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundAmount(@Param("saleItemId") UUID saleItemId);
}
