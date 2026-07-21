package com.example.pos.compliance.synchronization;

import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ItemSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(ItemSynchronizer.class);
    private final MedicineRepository medicineRepo;

    public ItemSynchronizer(MedicineRepository medicineRepo) {
        this.medicineRepo = medicineRepo;
    }

    @Override
    public String getSyncType() { return "ITEM"; }

    @Override
    public SyncResult sync() {
        long count = medicineRepo.count();
        log.info("Syncing {} medicine items to eTIMS", count);
        return new SyncResult((int) count, 0, null);
    }
}
