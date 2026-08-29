package com.example.pos.terminal.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.terminal.barcode.BarcodeType;
import com.example.pos.terminal.printer.BarcodePrintJob;
import com.example.pos.terminal.printer.LabelTemplate;
import com.example.pos.terminal.printer.PrintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final PrintService printService;
    private final MedicineRepository medicineRepo;

    public LabelController(PrintService printService, MedicineRepository medicineRepo) {
        this.printService = printService;
        this.medicineRepo = medicineRepo;
    }

    @PostMapping("/render")
    public ResponseEntity<ApiResponse<Map<String, String>>> render(@RequestBody LabelRequest request) {
        Medicine medicine = medicineRepo.findById(UUID.fromString(request.medicineId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Medicine not found"));

        LabelTemplate template = switch (request.templateSize() != null ? request.templateSize() : "STANDARD") {
            case "SMALL" -> LabelTemplate.SMALL_25x15;
            case "LARGE" -> LabelTemplate.LARGE_75x50;
            case "SHELF" -> LabelTemplate.SHELF_100x30;
            default -> LabelTemplate.STANDARD_50x25;
        };

        BarcodePrintJob job = new BarcodePrintJob()
                .template(template)
                .copies(request.copies() != null ? request.copies() : 1)
                .barcodeValue(medicine.getSku())
                .barcodeType(BarcodeType.CODE128)
                .medicineName(medicine.getBrandName())
                .genericName(medicine.getGenericName())
                .strength(medicine.getStrength())
                .price(medicine.getSellingPrice())
                .batchNumber(request.batchNumber())
                .expiryDate(request.expiryDate() != null ? request.expiryDate() : null);

        if (request.shelfLocation() != null && !request.shelfLocation().isBlank()) {
            job.addLine("Shelf", request.shelfLocation());
        }

        String rendered = printService.renderLabel(job);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "rendered", rendered != null ? rendered : "",
                "medicineName", medicine.getBrandName(),
                "sku", medicine.getSku()
        )));
    }

    public record LabelRequest(
            String medicineId,
            String templateSize,
            Integer copies,
            String batchNumber,
            LocalDate expiryDate,
            String shelfLocation
    ) {}
}
