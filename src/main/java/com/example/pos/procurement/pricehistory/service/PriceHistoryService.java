package com.example.pos.procurement.pricehistory.service;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.pricehistory.repository.PriceHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public Page<PriceHistory> getByMedicine(Long medicineId, Pageable pageable) {
        List<PriceHistory> list = repo.findByMedicineId(medicineId);
        return new PageImpl<>(list, pageable, list.size());
    }

    public Page<PriceHistory> getByBatch(Long batchId, Pageable pageable) {
        List<PriceHistory> list = repo.findByMedicineBatchesId(batchId);
        return new PageImpl<>(list, pageable, list.size());
    }
}
