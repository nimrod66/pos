package com.example.pos.compliance.certification;

import com.example.pos.compliance.event.repository.ComplianceEventRepository;
import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ArtifactExporter {

    private static final Logger log = LoggerFactory.getLogger(ArtifactExporter.class);

    private final TaxInvoiceRepository invoiceRepo;
    private final TransmissionRepository transmissionRepo;
    private final ComplianceEventRepository eventRepo;

    public ArtifactExporter(TaxInvoiceRepository invoiceRepo,
                            TransmissionRepository transmissionRepo,
                            ComplianceEventRepository eventRepo) {
        this.invoiceRepo = invoiceRepo;
        this.transmissionRepo = transmissionRepo;
        this.eventRepo = eventRepo;
    }

    public String exportAll() {
        log.info("Exporting certification artifacts...");
        long invoiceCount = invoiceRepo.count();
        long transmissionCount = transmissionRepo.count();
        long eventCount = eventRepo.count();

        return String.format(
                "Certification artifacts exported: %d invoices, %d transmissions, %d events at %s",
                invoiceCount, transmissionCount, eventCount, LocalDateTime.now()
        );
    }
}
