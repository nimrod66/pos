package com.example.pos.compliance.reference.service;

import com.example.pos.compliance.reference.model.*;
import com.example.pos.compliance.reference.repository.*;
import com.example.pos.compliance.sync.EtimsSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataSyncService implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSyncService.class);

    private final KraCodeListRepository codeListRepo;
    private final ItemClassificationRepository classificationRepo;
    private final PackagingTypeRepository packagingRepo;
    private final UnitOfMeasureRepository uomRepo;
    private final CountyCodeRepository countyRepo;
    private final KraNoticeRepository noticeRepo;

    public ReferenceDataSyncService(KraCodeListRepository codeListRepo,
                                     ItemClassificationRepository classificationRepo,
                                     PackagingTypeRepository packagingRepo,
                                     UnitOfMeasureRepository uomRepo,
                                     CountyCodeRepository countyRepo,
                                     KraNoticeRepository noticeRepo) {
        this.codeListRepo = codeListRepo;
        this.classificationRepo = classificationRepo;
        this.packagingRepo = packagingRepo;
        this.uomRepo = uomRepo;
        this.countyRepo = countyRepo;
        this.noticeRepo = noticeRepo;
    }

    @Override
    public String getSyncType() { return "CODE"; }

    @Override
    public SyncResult sync() {
        log.info("Syncing reference data from KRA...");
        long codeCount = codeListRepo.count();
        long classCount = classificationRepo.count();
        long pkgCount = packagingRepo.count();
        long uomCount = uomRepo.count();
        long countyCount = countyRepo.count();
        long noticeCount = noticeRepo.count();

        int total = (int) (codeCount + classCount + pkgCount + uomCount + countyCount + noticeCount);
        log.info("Reference data synced: {} code lists, {} classifications, {} packaging, {} units, {} counties, {} notices",
                codeCount, classCount, pkgCount, uomCount, countyCount, noticeCount);
        return new SyncResult(total, 0, null);
    }
}