package com.example.pos.procurement.pricehistory.repository;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByMedicineId(Long medicineId);
    List<PriceHistory> findByMedicineBatchesId(Long batchId);
}
