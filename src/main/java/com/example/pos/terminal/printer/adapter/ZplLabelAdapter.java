package com.example.pos.terminal.printer.adapter;

import com.example.pos.terminal.barcode.BarcodeType;
import com.example.pos.terminal.printer.BarcodePrintJob;
import com.example.pos.terminal.printer.LabelPrinter;
import com.example.pos.terminal.printer.LabelTemplate;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ZplLabelAdapter implements LabelPrinter {

    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String render(BarcodePrintJob job) {
        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n");

        int dpi = 203;
        int width = job.template().widthMm() * dpi / 25;
        int height = job.template().heightMm() * dpi / 25;

        zpl.append("^PW").append(width).append("\n");
        zpl.append("^LL").append(height).append("\n");
        zpl.append("^CF0,30\n");

        int y = 20;
        if (job.medicineName() != null) {
            zpl.append("^FO10,").append(y).append("^A0N,25,25^FD").append(escapeZpl(job.medicineName())).append("^FS\n");
            y += 30;
        }
        if (job.strength() != null) {
            zpl.append("^FO10,").append(y).append("^A0N,20,20^FD").append(escapeZpl(job.strength())).append("^FS\n");
            y += 25;
        }

        String bcValue = job.barcodeValue() != null ? job.barcodeValue() : "0";
        BarcodeType bcType = job.barcodeType() != null ? job.barcodeType() : BarcodeType.CODE128;
        String bcCommand = toZplBarcodeCommand(bcType, bcValue, height);
        zpl.append("^FO10,").append(y).append(bcCommand).append("^FS\n");
        y += 80;

        if (job.price() != null) {
            zpl.append("^FO10,").append(y).append("^A0N,22,22^FDKSh ").append(job.price().toPlainString()).append("^FS\n");
            y += 30;
        }
        if (job.batchNumber() != null) {
            zpl.append("^FO10,").append(y).append("^A0N,18,18^FDBatch: ").append(escapeZpl(job.batchNumber())).append("^FS\n");
            y += 22;
        }
        if (job.expiryDate() != null) {
            zpl.append("^FO10,").append(y).append("^A0N,18,18^FDExp: ").append(job.expiryDate().format(EXPIRY_FMT)).append("^FS\n");
            y += 22;
        }
        for (BarcodePrintJob.JobLine line : job.additionalLines()) {
            zpl.append("^FO10,").append(y).append("^A0N,16,16^FD").append(escapeZpl(line.label())).append(": ").append(escapeZpl(line.value())).append("^FS\n");
            y += 18;
        }

        zpl.append("^PQ").append(Math.max(1, job.copies())).append(",0,1,Y\n");
        zpl.append("^XZ\n");
        return zpl.toString();
    }

    @Override
    public boolean supports(LabelTemplate template) {
        return true;
    }

    @Override
    public String printerLanguage() {
        return "ZPL";
    }

    private String toZplBarcodeCommand(BarcodeType type, String value, int labelHeight) {
        return switch (type) {
            case CODE128 -> "^BCN,50,Y,N,N,A";
            case CODE39 -> "^B3N,N,50,Y,N";
            case EAN13 -> "^BEN,50,Y,N";
            case EAN8 -> "^B8N,50,Y,N";
            case UPC_A -> "^BUN,50,Y,N";
            case QR_CODE -> "^BQN,2,6";
            case GS1_DATAMATRIX -> "^BXN,6,200";
            case PDF417 -> "^B7N,6,0,0,0,N";
            case ITF14 -> "^B2N,50,Y,N,N";
            default -> "^BCN,50,Y,N,N,A";
        };
    }

    private String escapeZpl(String text) {
        if (text == null) return "";
        return text.replace("^", "\\^").replace("~", "\\~");
    }
}
