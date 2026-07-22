package com.example.pos.terminal.printer;

public interface LabelPrinter {
    String render(BarcodePrintJob job);
    boolean supports(LabelTemplate template);
    String printerLanguage();
}
