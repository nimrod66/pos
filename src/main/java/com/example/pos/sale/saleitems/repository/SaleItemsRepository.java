package com.example.pos.sale.saleitems.repository;

import java.util.UUID;

import com.example.pos.sale.saleitems.model.SaleItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.pos.sale.sales.model.Sales;
import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;

public interface SaleItemsRepository extends JpaRepository<SaleItems, UUID> {

    @Query("select coalesce(sum(item.quantity), 0) from SaleReturnItems item "
            + "where item.saleItems.id = :saleItemId and item.saleReturns.status = 'COMPLETED'")
    int sumCompletedReturnQuantity(@Param("saleItemId") UUID saleItemId);

    @Query("select coalesce(sum(item.refundAmount), 0) from SaleReturnItems item "
            + "where item.saleItems.id = :saleItemId and item.saleReturns.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundAmount(@Param("saleItemId") UUID saleItemId);


    @Query("select item.medicineBatches.medicine.id as medicineId, "
         + "max(item.medicineBatches.medicine.brandName) as medicineName, "
         + "max(item.medicineBatches.medicine.sku) as sku, "
         + "max(item.price) as unitPrice, "
         + "sum(item.quantity) as quantitySold, "
         + "sum(item.total) as revenue "
         + "from SaleItems item "
         + "where item.sales.branch.id = :branchId "
         + "and item.sales.saleStatus in :statuses "
         + "and item.sales.completedAt >= :from and item.sales.completedAt < :to "
         + "group by item.medicineBatches.medicine.id "
         + "order by sum(item.quantity) desc")
    List<PluProjection> aggregatePlu(
            @Param("branchId") UUID branchId,
            @Param("statuses") List<Sales.SaleStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    interface PluProjection {
        UUID getMedicineId();
        String getMedicineName();
        String getSku();
        java.math.BigDecimal getUnitPrice();
        Long getQuantitySold();
        java.math.BigDecimal getRevenue();
    }
}
