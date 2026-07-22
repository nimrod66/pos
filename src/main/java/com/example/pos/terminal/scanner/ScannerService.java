package com.example.pos.terminal.scanner;

import com.example.pos.terminal.barcode.BarcodeParser;
import com.example.pos.terminal.barcode.BarcodeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ScannerService {

    private final BarcodeParser barcodeParser;
    private final List<ScannerProvider> providers;

    public ScannerService(BarcodeParser barcodeParser, List<ScannerProvider> providers) {
        this.barcodeParser = barcodeParser;
        this.providers = providers;
    }

    public void registerProvider(ScannerProvider provider) {
        providers.add(provider);
    }

    public ScanResult scan(String rawInput, String terminalId) {
        if (rawInput == null || rawInput.isBlank()) {
            return ScanResult.failed("Empty scan input", "UNKNOWN", terminalId);
        }

        BarcodeParser.ParseResult parsed = barcodeParser.parse(rawInput);
        if (!parsed.detected()) {
            return ScanResult.failed(parsed.error(), "UNKNOWN", terminalId);
        }

        return ScanResult.success(parsed.value(), parsed.type(), "DETECTED", terminalId);
    }

    public ScanResult scan(String rawInput, String scannerType, String terminalId) {
        Optional<ScannerProvider> provider = providers.stream()
                .filter(p -> p.getScannerType().equalsIgnoreCase(scannerType) && p.isAvailable())
                .findFirst();

        if (provider.isPresent()) {
            return provider.get().scan(rawInput, terminalId);
        }

        return scan(rawInput, terminalId);
    }

    public List<String> availableScannerTypes() {
        return providers.stream()
                .filter(ScannerProvider::isAvailable)
                .map(ScannerProvider::getScannerType)
                .distinct()
                .toList();
    }

    public boolean isScannerAvailable(String scannerType) {
        return providers.stream()
                .anyMatch(p -> p.getScannerType().equalsIgnoreCase(scannerType) && p.isAvailable());
    }
}
