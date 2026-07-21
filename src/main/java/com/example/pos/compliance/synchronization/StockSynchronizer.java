package com.example.pos.compliance.synchronization;

import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StockSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(StockSynchronizer.class);
    private final StockMovementsRepository stockMovementsRepo;

    public StockSynchronizer(StockMovementsRepository stockMovementsRepo) {
        this.stockMovementsRepo = stockMovementsRepo;
    }

    @Override
    public String getSyncType() { return "STOCK"; }

    @Override
    public SyncResult sync() {
        long count = stockMovementsRepo.count();
        log.info("Syncing {} stock movements to eTIMS", count);
        return new SyncResult((int) count, 0, null);
    }
}
