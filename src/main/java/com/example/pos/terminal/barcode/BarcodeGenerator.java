package com.example.pos.terminal.barcode;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class BarcodeGenerator {

    private static final String CODE128_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    public String generateInternalBarcode() {
        String prefix = "SYS";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String randomPart = randomCode128(5);
        return prefix + datePart + randomPart;
    }

    public String generateInternalBarcode(String companyPrefix) {
        if (companyPrefix == null || companyPrefix.length() < 3) {
            return generateInternalBarcode();
        }
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String randomPart = randomCode128(5);
        return companyPrefix.substring(0, Math.min(companyPrefix.length(), 6)).toUpperCase() + datePart + randomPart;
    }

    public String generateEan13(String companyPrefix, int productCode) {
        if (companyPrefix == null || companyPrefix.length() > 10) {
            throw new IllegalArgumentException("Company prefix must be 1-10 digits");
        }
        int prefixLen = companyPrefix.length();
        int productLen = 11 - prefixLen;
        String productStr = String.format("%0" + productLen + "d", productCode % (int) Math.pow(10, productLen));
        String base = companyPrefix + productStr;
        int checkDigit = computeMod10CheckDigit(base);
        return base + checkDigit;
    }

    public String generateUpcA(String companyPrefix, int productCode) {
        return generateEan13("0" + companyPrefix, productCode).substring(1);
    }

    public int computeMod10CheckDigit(String base) {
        int sum = 0;
        boolean alternate = false;
        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(base.charAt(i));
            if (digit < 0 || digit > 9) throw new IllegalArgumentException("Non-numeric character in base: " + base.charAt(i));
            if (alternate) {
                digit *= 3;
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    private String randomCode128(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE128_CHARS.charAt(random.nextInt(CODE128_CHARS.length())));
        }
        return sb.toString();
    }
}
