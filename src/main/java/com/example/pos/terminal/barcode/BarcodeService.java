package com.example.pos.terminal.barcode;

import com.example.pos.masterdata.medicine.model.Medicine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BarcodeService {

    private final BarcodeGenerator generator;
    private final BarcodeValidator validator;
    private final BarcodeParser parser;
    private final BarcodeSymbology symbology;

    public BarcodeService(BarcodeGenerator generator, BarcodeValidator validator,
                          BarcodeParser parser, BarcodeSymbology symbology) {
        this.generator = generator;
        this.validator = validator;
        this.parser = parser;
        this.symbology = symbology;
    }

    public String primaryBarcode(Medicine medicine) {
        if (medicine.getManufacturerBarcode() != null && !medicine.getManufacturerBarcode().isBlank()) {
            return medicine.getManufacturerBarcode();
        }
        if (medicine.getInternalBarcode() != null && !medicine.getInternalBarcode().isBlank()) {
            return medicine.getInternalBarcode();
        }
        return medicine.getBarcode();
    }

    public String generateFor(Medicine medicine) {
        if (medicine.getManufacturerBarcode() != null && !medicine.getManufacturerBarcode().isBlank()) {
            return medicine.getManufacturerBarcode();
        }

        if (medicine.getInternalBarcode() != null && !medicine.getInternalBarcode().isBlank()) {
            return medicine.getInternalBarcode();
        }

        String gs1Prefix = medicine.getGs1CompanyPrefix();
        if (gs1Prefix != null && !gs1Prefix.isBlank()) {
            int productCode = medicine.getId() != null ? medicine.getId().intValue() : 1;
            return generator.generateEan13(gs1Prefix, productCode);
        }

        return generator.generateInternalBarcode();
    }

    public BarcodeValidator.ValidationResult validate(String value, BarcodeType type) {
        return validator.validate(value, type);
    }

    public boolean isValid(String value, BarcodeType type) {
        return validator.hasValidFormat(value, type);
    }

    public BarcodeParser.ParseResult parse(String raw) {
        return parser.parse(raw);
    }

    public BarcodeType detectType(String raw) {
        return parser.detectType(raw);
    }

    public String generateInternalCode() {
        return generator.generateInternalBarcode();
    }

    public String generateInternalCode(String companyPrefix) {
        return generator.generateInternalBarcode(companyPrefix);
    }

    public String generateEan13(String companyPrefix, int productCode) {
        return generator.generateEan13(companyPrefix, productCode);
    }

    public BarcodeSymbology.Rules rulesFor(BarcodeType type) {
        return symbology.forType(type);
    }
}
