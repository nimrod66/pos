package com.example.pos.compliance.synchronization;

import com.example.pos.core.branch.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BranchSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(BranchSynchronizer.class);
    private final BranchRepository branchRepo;

    public BranchSynchronizer(BranchRepository branchRepo) {
        this.branchRepo = branchRepo;
    }

    @Override
    public String getSyncType() { return "BRANCH"; }

    @Override
    public SyncResult sync() {
        long count = branchRepo.count();
        log.info("Syncing {} branches to eTIMS", count);
        return new SyncResult((int) count, 0, null);
    }
}
