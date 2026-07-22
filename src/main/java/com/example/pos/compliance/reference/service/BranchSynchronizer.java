package com.example.pos.compliance.reference.service;

import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BranchSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(BranchSynchronizer.class);

    @Override
    public String getSyncType() { return "BRANCH"; }

    @Override
    public SyncResult sync() {
        log.info("Branch synchronization — stub.");
        return new SyncResult(0, 0, null);
    }
}