package com.example.pos.terminal.scanner;

import com.example.pos.terminal.barcode.BarcodeType;

import java.time.Instant;

public record ScanResult(
        String barcode,
        BarcodeType symbology,
        Instant timestamp,
        String scannerType,
        String terminalId,
        boolean detected,
        String error
) {
    public static ScanResult success(String barcode, BarcodeType symbology, String scannerType, String terminalId) {
        return new ScanResult(barcode, symbology, Instant.now(), scannerType, terminalId, true, null);
    }

    public static ScanResult failed(String error, String scannerType, String terminalId) {
        return new ScanResult(null, null, Instant.now(), scannerType, terminalId, false, error);
    }
}
