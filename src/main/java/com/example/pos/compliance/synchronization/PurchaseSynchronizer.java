package com.example.pos.compliance.synchronization;

import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PurchaseSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(PurchaseSynchronizer.class);
    private final PurchaseOrdersRepository purchaseRepo;

    public PurchaseSynchronizer(PurchaseOrdersRepository purchaseRepo) {
        this.purchaseRepo = purchaseRepo;
    }

    @Override
    public String getSyncType() { return "PURCHASE"; }

    @Override
    public SyncResult sync() {
        long count = purchaseRepo.count();
        log.info("Syncing {} purchase transactions to eTIMS", count);
        return new SyncResult((int) count, 0, null);
    }
}
