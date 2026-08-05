package com.example.pos.procurement.pricehistory.repository;

import java.util.UUID;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByMedicineId(UUID medicineId);
    List<PriceHistory> findByMedicineBatchesId(UUID batchId);
}
