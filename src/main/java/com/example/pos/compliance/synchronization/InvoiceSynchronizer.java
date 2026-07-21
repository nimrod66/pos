package com.example.pos.compliance.synchronization;

import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InvoiceSynchronizer implements EtimsSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceSynchronizer.class);
    private final TaxInvoiceRepository invoiceRepo;
    private final TransmissionRepository transmissionRepo;

    public InvoiceSynchronizer(TaxInvoiceRepository invoiceRepo, TransmissionRepository transmissionRepo) {
        this.invoiceRepo = invoiceRepo;
        this.transmissionRepo = transmissionRepo;
    }

    @Override
    public String getSyncType() { return "INVOICE"; }

    @Override
    public SyncResult sync() {
        long total = invoiceRepo.count();
        long pending = transmissionRepo.countByTransmissionStatus(TransmissionStatus.TRANSMITTED);
        log.info("Invoice sync: {} total, {} transmitted to KRA", total, pending);
        return new SyncResult((int) total, 0, null);
    }
}
