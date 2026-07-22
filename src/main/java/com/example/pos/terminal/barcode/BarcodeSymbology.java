package com.example.pos.terminal.barcode;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BarcodeSymbology {

    private final Map<BarcodeType, Rules> registry = new EnumMap<>(BarcodeType.class);

    public BarcodeSymbology() {
        register(BarcodeType.EAN13, 13, "^[0-9]{13}$", CheckDigitAlgorithm.MOD10, "EAN-13 International Article Number");
        register(BarcodeType.EAN8, 8, "^[0-9]{8}$", CheckDigitAlgorithm.MOD10, "EAN-8 International Article Number (short)");
        register(BarcodeType.UPC_A, 12, "^[0-9]{12}$", CheckDigitAlgorithm.MOD10, "UPC-A Universal Product Code");
        register(BarcodeType.UPC_E, 6, "^[0-9]{6}$", CheckDigitAlgorithm.MOD10, "UPC-E Universal Product Code (compressed)");
        register(BarcodeType.CODE128, -1, "^[\\u0020-\\u007E]+$", null, "Code 128 variable-length alphanumeric");
        register(BarcodeType.CODE39, -1, "^[A-Z0-9 \\-.$/+%]*$", null, "Code 39 variable-length uppercase");
        register(BarcodeType.CODE93, -1, "^[A-Z0-9 \\-.$/+%]*$", null, "Code 93 compressed Code 39");
        register(BarcodeType.ITF14, 14, "^[0-9]{14}$", CheckDigitAlgorithm.MOD10, "ITF-14 trade unit identifier");
        register(BarcodeType.QR_CODE, -1, null, null, "QR Code 2D matrix");
        register(BarcodeType.GS1_DATAMATRIX, -1, null, null, "GS1 DataMatrix 2D matrix");
        register(BarcodeType.GS1_128, -1, "^[\\u0020-\\u007E]+$", null, "GS1-128 (Application Identifier format)");
        register(BarcodeType.CODABAR, -1, "^[A-D][0-9\\-.$/:+]+[A-D]$", null, "Codabar (blood banks, libraries)");
        register(BarcodeType.PDF417, -1, null, null, "PDF417 2D stacked linear");
    }

    private void register(BarcodeType type, int fixedLength, String pattern, CheckDigitAlgorithm algorithm, String description) {
        registry.put(type, new Rules(fixedLength, pattern, algorithm, description));
    }

    public Rules forType(BarcodeType type) {
        return registry.get(type);
    }

    public boolean hasFixedLength(BarcodeType type) {
        Rules r = registry.get(type);
        return r != null && r.fixedLength > 0;
    }

    public int fixedLength(BarcodeType type) {
        Rules r = registry.get(type);
        if (r == null || r.fixedLength <= 0) {
            throw new IllegalArgumentException("No fixed length for " + type);
        }
        return r.fixedLength;
    }

    public record Rules(int fixedLength, String pattern, CheckDigitAlgorithm algorithm, String description) {}

    public enum CheckDigitAlgorithm {
        MOD10,
        GS1_MOD10,
        NONE
    }
}
