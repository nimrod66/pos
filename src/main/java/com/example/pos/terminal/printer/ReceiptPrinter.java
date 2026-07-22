package com.example.pos.terminal.printer;

public interface ReceiptPrinter {
    String renderReceipt(com.example.pos.compliance.receipt.dto.ReceiptDTO receipt);
    boolean supportsReceiptPrinting();
    String printerLanguage();
}
