package com.example.pos.compliance.invoice.service;

import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InvoiceSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceSynchronizer.class);

    @Override
    public String getSyncType() { return "INVOICE"; }

    @Override
    public SyncResult sync() {
        log.info("Invoice synchronization — stub.");
        return new SyncResult(0, 0, null);
    }
}