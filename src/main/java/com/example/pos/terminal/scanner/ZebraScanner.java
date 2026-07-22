package com.example.pos.terminal.scanner;

import com.example.pos.terminal.barcode.BarcodeParser;
import org.springframework.stereotype.Component;

@Component
public class ZebraScanner implements ScannerProvider {

    private final BarcodeParser barcodeParser;

    public ZebraScanner(BarcodeParser barcodeParser) {
        this.barcodeParser = barcodeParser;
    }

    @Override
    public String getName() {
        return "Zebra Scanner";
    }

    @Override
    public String getScannerType() {
        return "ZEBRA";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ScanResult scan(String rawInput, String terminalId) {
        if (rawInput == null || rawInput.isBlank()) {
            return ScanResult.failed("Empty Zebra scan input", getScannerType(), terminalId);
        }
        var parsed = barcodeParser.parse(rawInput);
        if (!parsed.detected()) {
            return ScanResult.failed("Zebra decode failed: " + parsed.error(), getScannerType(), terminalId);
        }
        return ScanResult.success(parsed.value(), parsed.type(), getScannerType(), terminalId);
    }
}
