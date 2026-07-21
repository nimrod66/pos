package com.example.pos.procurement.pricehistory.service;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.pricehistory.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PriceHistoryService {

    private final PriceHistoryRepository repo;

    public PriceHistoryService(PriceHistoryRepository repo) {
        this.repo = repo;
    }

    public List<PriceHistory> getByMedicine(Long medicineId) {
        return repo.findByMedicineId(medicineId);
    }

    public List<PriceHistory> getByBatch(Long batchId) {
        return repo.findByMedicineBatchesId(batchId);
    }
}
