package com.example.pos.compliance.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class SyncEngine {

    private static final Logger log = LoggerFactory.getLogger(SyncEngine.class);
    private final List<EtimsSynchronizer> synchronizers;
    private final SyncStateRepository stateRepo;

    public SyncEngine(List<EtimsSynchronizer> synchronizers, SyncStateRepository stateRepo) {
        this.synchronizers = synchronizers.stream()
                .sorted(Comparator.comparing(EtimsSynchronizer::getSyncType))
                .toList();
        this.stateRepo = stateRepo;
    }

    public void runAll() {
        log.info("Starting full eTIMS synchronization");
        for (EtimsSynchronizer sync : synchronizers) {
            runSynchronizer(sync);
        }
        log.info("Full eTIMS synchronization complete");
    }

    public void runCodeSync() { runByType("CODE"); }
    public void runItemSync() { runByType("ITEM"); }
    public void runBranchSync() { runByType("BRANCH"); }
    public void runPurchaseSync() { runByType("PURCHASE"); }
    public void runStockSync() { runByType("STOCK"); }
    public void runInvoiceSync() { runByType("INVOICE"); }

    private void runByType(String type) {
        synchronizers.stream()
                .filter(s -> s.getSyncType().equals(type))
                .findFirst()
                .ifPresent(this::runSynchronizer);
    }

    private void runSynchronizer(EtimsSynchronizer synchronizer) {
        log.info("Running synchronizer: {}", synchronizer.getSyncType());
        var result = synchronizer.sync();
        updateState(synchronizer.getSyncType(), result);
    }

    private void updateState(String syncType, EtimsSynchronizer.SyncResult result) {
        SyncState state = stateRepo.findBySyncTypeAndTenantId(syncType, null)
                .orElseGet(() -> SyncState.builder().syncType(syncType).build());
        state.setLastSyncAt(LocalDateTime.now());
        state.setLastSyncStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        state.setRecordsSynced(result.synced());
        state.setRecordsFailed(result.failed());
        state.setErrorMessage(result.error());
        stateRepo.save(state);
    }
}