package com.example.pos.compliance.catalog.service;

import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CatalogSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(CatalogSynchronizer.class);

    @Override
    public String getSyncType() { return "ITEM"; }

    @Override
    public SyncResult sync() {
        log.info("Catalog item synchronization — stub. Items registered with KRA via reference data.");
        return new SyncResult(0, 0, null);
    }
}