package com.example.pos.terminal.scanner;

public interface ScannerProvider {
    String getName();
    String getScannerType();
    boolean isAvailable();
    ScanResult scan(String rawInput, String terminalId);
}
