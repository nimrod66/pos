package com.example.pos.terminal.printer.adapter;

import com.example.pos.terminal.barcode.BarcodeType;
import com.example.pos.terminal.printer.BarcodePrintJob;
import com.example.pos.terminal.printer.LabelPrinter;
import com.example.pos.terminal.printer.LabelTemplate;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TsplLabelAdapter implements LabelPrinter {

    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String render(BarcodePrintJob job) {
        StringBuilder tspl = new StringBuilder();
        tspl.append("SIZE ").append(job.template().widthMm()).append(" mm,")
                .append(job.template().heightMm()).append(" mm\n");
        tspl.append("GAP 2 mm,0 mm\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("CLS\n");

        int y = 20;
        if (job.medicineName() != null) {
            tspl.append("TEXT 10,").append(y).append(",\"1\",0,1,1,\"").append(job.medicineName()).append("\"\n");
            y += 30;
        }
        if (job.strength() != null) {
            tspl.append("TEXT 10,").append(y).append(",\"1\",0,1,1,\"").append(job.strength()).append("\"\n");
            y += 25;
        }

        String bcValue = job.barcodeValue() != null ? job.barcodeValue() : "0";
        BarcodeType bcType = job.barcodeType() != null ? job.barcodeType() : BarcodeType.CODE128;
        String barcodeCommand = toTsplBarcodeCommand(bcType, bcValue);
        tspl.append("BARCODE 10,").append(y).append(",\"").append(barcodeCommand).append("\",50,1,0,2,2,\"").append(bcValue).append("\"\n");
        y += 80;

        if (job.price() != null) {
            tspl.append("TEXT 10,").append(y).append(",\"1\",0,1,1,\"KSh ").append(job.price().toPlainString()).append("\"\n");
            y += 30;
        }
        if (job.batchNumber() != null) {
            tspl.append("TEXT 10,").append(y).append(",\"1\",0,1,1,\"Batch: ").append(job.batchNumber()).append("\"\n");
            y += 22;
        }
        if (job.expiryDate() != null) {
            tspl.append("TEXT 10,").append(y).append(",\"1\",0,1,1,\"Exp: ").append(job.expiryDate().format(EXPIRY_FMT)).append("\"\n");
            y += 22;
        }

        tspl.append("PRINT ").append(Math.max(1, job.copies())).append(",1\n");
        return tspl.toString();
    }

    @Override
    public boolean supports(LabelTemplate template) {
        return true;
    }

    @Override
    public String printerLanguage() {
        return "TSPL";
    }

    private String toTsplBarcodeCommand(BarcodeType type, String value) {
        return switch (type) {
            case CODE128 -> "128";
            case CODE39 -> "39";
            case EAN13 -> "EAN13";
            case EAN8 -> "EAN8";
            case UPC_A -> "UPCA";
            case QR_CODE -> "QRCODE";
            case ITF14 -> "ITF14";
            case CODABAR -> "CODABAR";
            default -> "128";
        };
    }
}
