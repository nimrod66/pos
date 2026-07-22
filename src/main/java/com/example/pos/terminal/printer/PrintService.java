package com.example.pos.terminal.printer;

import com.example.pos.compliance.receipt.dto.ReceiptDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrintService {

    private final List<LabelPrinter> labelPrinters;
    private final List<ReceiptPrinter> receiptPrinters;

    public PrintService(List<LabelPrinter> labelPrinters, List<ReceiptPrinter> receiptPrinters) {
        this.labelPrinters = labelPrinters;
        this.receiptPrinters = receiptPrinters;
    }

    public String renderLabel(BarcodePrintJob job) {
        for (LabelPrinter printer : labelPrinters) {
            if (printer.supports(job.template())) {
                return printer.render(job);
            }
        }
        return labelPrinters.isEmpty() ? null : labelPrinters.get(0).render(job);
    }

    public String renderLabel(BarcodePrintJob job, String printerLanguage) {
        for (LabelPrinter printer : labelPrinters) {
            if (printer.printerLanguage().equalsIgnoreCase(printerLanguage)
                    && printer.supports(job.template())) {
                return printer.render(job);
            }
        }
        return renderLabel(job);
    }

    public String renderReceipt(ReceiptDTO receipt) {
        for (ReceiptPrinter printer : receiptPrinters) {
            if (printer.supportsReceiptPrinting()) {
                return printer.renderReceipt(receipt);
            }
        }
        return receiptPrinters.isEmpty() ? null : receiptPrinters.get(0).renderReceipt(receipt);
    }

    public List<String> availableLabelLanguages() {
        return labelPrinters.stream().map(LabelPrinter::printerLanguage).distinct().toList();
    }

    public List<String> availableReceiptLanguages() {
        return receiptPrinters.stream().map(ReceiptPrinter::printerLanguage).distinct().toList();
    }
}
