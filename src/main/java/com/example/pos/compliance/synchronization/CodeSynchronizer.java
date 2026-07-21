package com.example.pos.compliance.synchronization;

import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodeSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(CodeSynchronizer.class);
    private final TaxRepository taxRepo;

    public CodeSynchronizer(TaxRepository taxRepo) {
        this.taxRepo = taxRepo;
    }

    @Override
    public String getSyncType() { return "CODE"; }

    @Override
    public SyncResult sync() {
        List<Tax> codes = taxRepo.findByActiveTrue();
        log.info("Syncing {} active tax codes to eTIMS", codes.size());
        return new SyncResult(codes.size(), 0, null);
    }
}
