package com.example.pos.terminal.barcode;

import org.springframework.stereotype.Component;

@Component
public class BarcodeValidator {

    private final BarcodeSymbology symbology;

    public BarcodeValidator(BarcodeSymbology symbology) {
        this.symbology = symbology;
    }

    public ValidationResult validate(String value, BarcodeType type) {
        if (value == null || value.isBlank()) {
            return ValidationResult.invalid("Barcode value is blank");
        }
        BarcodeSymbology.Rules rules = symbology.forType(type);
        if (rules == null) {
            return ValidationResult.invalid("Unknown barcode type: " + type);
        }

        if (rules.pattern() != null && !value.matches(rules.pattern())) {
            return ValidationResult.invalid("Barcode does not match " + type + " pattern");
        }

        if (rules.fixedLength() > 0 && value.length() != rules.fixedLength()) {
            return ValidationResult.invalid("Barcode length " + value.length() + " does not match expected " + rules.fixedLength() + " for " + type);
        }

        if (rules.algorithm() == BarcodeSymbology.CheckDigitAlgorithm.MOD10
                || rules.algorithm() == BarcodeSymbology.CheckDigitAlgorithm.GS1_MOD10) {
            if (!verifyMod10CheckDigit(value)) {
                return ValidationResult.invalid("Mod10 check digit verification failed for " + type);
            }
        }

        return ValidationResult.valid(value, type);
    }

    public boolean hasValidFormat(String value, BarcodeType type) {
        return validate(value, type).valid();
    }

    private boolean verifyMod10CheckDigit(String value) {
        if (value == null || value.length() < 2) return false;
        try {
            int sum = 0;
            boolean alternate = false;
            for (int i = value.length() - 2; i >= 0; i--) {
                int digit = Character.getNumericValue(value.charAt(i));
                if (digit < 0 || digit > 9) return false;
                if (alternate) {
                    digit *= 3;
                }
                sum += digit;
                alternate = !alternate;
            }
            int checkDigit = (10 - (sum % 10)) % 10;
            return checkDigit == Character.getNumericValue(value.charAt(value.length() - 1));
        } catch (Exception e) {
            return false;
        }
    }

    public record ValidationResult(String value, BarcodeType type, boolean valid, String error) {
        public static ValidationResult valid(String value, BarcodeType type) {
            return new ValidationResult(value, type, true, null);
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(null, null, false, error);
        }
    }
}
