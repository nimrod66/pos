package com.example.pos.terminal.printer;

import com.example.pos.terminal.barcode.BarcodeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BarcodePrintJob {

    private LabelTemplate template;
    private int copies;
    private String barcodeValue;
    private BarcodeType barcodeType;
    private String medicineName;
    private String genericName;
    private String strength;
    private BigDecimal price;
    private String batchNumber;
    private LocalDate expiryDate;
    private String additionalText;
    private final List<JobLine> additionalLines = new ArrayList<>();

    public BarcodePrintJob template(LabelTemplate template) { this.template = template; return this; }
    public BarcodePrintJob copies(int copies) { this.copies = copies; return this; }
    public BarcodePrintJob barcodeValue(String v) { this.barcodeValue = v; return this; }
    public BarcodePrintJob barcodeType(BarcodeType t) { this.barcodeType = t; return this; }
    public BarcodePrintJob medicineName(String v) { this.medicineName = v; return this; }
    public BarcodePrintJob genericName(String v) { this.genericName = v; return this; }
    public BarcodePrintJob strength(String v) { this.strength = v; return this; }
    public BarcodePrintJob price(BigDecimal v) { this.price = v; return this; }
    public BarcodePrintJob batchNumber(String v) { this.batchNumber = v; return this; }
    public BarcodePrintJob expiryDate(LocalDate v) { this.expiryDate = v; return this; }
    public BarcodePrintJob additionalText(String v) { this.additionalText = v; return this; }
    public BarcodePrintJob addLine(String label, String value) { this.additionalLines.add(new JobLine(label, value)); return this; }

    public LabelTemplate template() { return template; }
    public int copies() { return copies; }
    public String barcodeValue() { return barcodeValue; }
    public BarcodeType barcodeType() { return barcodeType; }
    public String medicineName() { return medicineName; }
    public String genericName() { return genericName; }
    public String strength() { return strength; }
    public BigDecimal price() { return price; }
    public String batchNumber() { return batchNumber; }
    public LocalDate expiryDate() { return expiryDate; }
    public String additionalText() { return additionalText; }
    public List<JobLine> additionalLines() { return List.copyOf(additionalLines); }

    public record JobLine(String label, String value) {}
}
