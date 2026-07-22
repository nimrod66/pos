package com.example.pos.compliance.stock.service;

import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StockSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(StockSynchronizer.class);

    @Override
    public String getSyncType() { return "STOCK"; }

    @Override
    public SyncResult sync() {
        log.info("Stock synchronization — stub.");
        return new SyncResult(0, 0, null);
    }
}