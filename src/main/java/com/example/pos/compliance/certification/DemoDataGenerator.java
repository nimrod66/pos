package com.example.pos.compliance.certification;

import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.model.TaxType;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);
    private final TaxRepository taxRepo;

    public DemoDataGenerator(TaxRepository taxRepo) {
        this.taxRepo = taxRepo;
    }

    public void generate() {
        log.info("Generating deterministic demo data for KRA certification...");
        ensureTaxCategories();
        log.info("Demo data generation complete");
    }

    private void ensureTaxCategories() {
        createIfMissing("VAT16", "VAT 16%", new BigDecimal("16.00"), TaxType.VAT_STANDARD);
        createIfMissing("VAT8", "VAT 8%", new BigDecimal("8.00"), TaxType.VAT_REDUCED);
        createIfMissing("VAT0", "VAT 0%", new BigDecimal("0.00"), TaxType.VAT_ZERO);
        createIfMissing("EXEMPT", "Exempt", BigDecimal.ZERO, TaxType.EXEMPT);
    }

    private void createIfMissing(String code, String name, BigDecimal rate, TaxType type) {
        if (taxRepo.findByCode(code).isEmpty()) {
            Tax tax = Tax.builder()
                    .code(code)
                    .taxName(name)
                    .taxDescription(name)
                    .taxRate(rate)
                    .taxType(type)
                    .active(true)
                    .build();
            taxRepo.save(tax);
            log.info("Created demo tax category: {}", code);
        }
    }
}
