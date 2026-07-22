package com.example.pos.compliance.purchases.service;

import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PurchaseSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(PurchaseSynchronizer.class);

    @Override
    public String getSyncType() { return "PURCHASE"; }

    @Override
    public SyncResult sync() {
        log.info("Purchase synchronization — stub.");
        return new SyncResult(0, 0, null);
    }
}