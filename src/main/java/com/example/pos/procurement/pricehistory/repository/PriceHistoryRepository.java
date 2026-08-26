package com.example.pos.procurement.pricehistory.repository;

import java.util.UUID;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"medicine", "user", "medicineBatches"})
    List<PriceHistory> findByMedicineId(UUID medicineId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"medicine", "user", "medicineBatches"})
    List<PriceHistory> findByMedicineBatchesId(UUID batchId);
}
