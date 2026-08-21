package com.example.pos.terminal.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.masterdata.medicine.dto.MedicineResponseDto;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.terminal.barcode.BarcodeService;
import com.example.pos.terminal.barcode.BarcodeType;
import com.example.pos.terminal.scanner.ScanResult;
import com.example.pos.terminal.scanner.ScannerService;
import com.example.pos.terminal.service.TerminalRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/terminals")
public class ScanController {

    private final ScannerService scannerService;
    private final BarcodeService barcodeService;
    private final MedicineRepository medicineRepository;
    private final AuthenticatedUserContext current;
    private final TerminalRegistrationService registrationService;

    public ScanController(ScannerService scannerService, BarcodeService barcodeService,
                          MedicineRepository medicineRepository,
                          AuthenticatedUserContext current,
                          TerminalRegistrationService registrationService) {
        this.scannerService = scannerService;
        this.barcodeService = barcodeService;
        this.medicineRepository = medicineRepository;
        this.current = current;
        this.registrationService = registrationService;
    }

    @PostMapping("/{terminalId}/scan")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scan(
            @PathVariable String terminalId,
            @RequestBody Map<String, String> body) {
        registrationService.getByTerminalId(terminalId);
        String rawBarcode = body.get("barcode");
        String scannerType = body.getOrDefault("scannerType", "KEYBOARD_WEDGE");

        ScanResult scanResult = scannerService.scan(rawBarcode, scannerType, terminalId);

        if (!scanResult.detected()) {
            return ResponseEntity.ok(ApiResponse.error(scanResult.error()));
        }

        var medicine = medicineRepository.findByPharmacyIdAndBarcode(
                        current.pharmacyId(), scanResult.barcode())
                .or(() -> medicineRepository.findByPharmacyIdAndBarcode(
                        current.pharmacyId(), scanResult.barcode().replaceAll("^0+", "")))
                .orElse(null);

        BarcodeType type = scanResult.symbology();

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("barcode", scanResult.barcode());
        data.put("barcodeType", type != null ? type.name() : null);
        data.put("symbology", type != null ? type.name() : null);
        data.put("scannerType", scanResult.scannerType());
        data.put("terminalId", terminalId);
        data.put("timestamp", scanResult.timestamp().toString());
        data.put("medicine", medicine != null ? MedicineResponseDto.from(medicine) : null);
        data.put("found", medicine != null);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{terminalId}/scanner-types")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell')")
    public ResponseEntity<ApiResponse<Object>> scannerTypes(@PathVariable String terminalId) {
        registrationService.getByTerminalId(terminalId);
        return ResponseEntity.ok(ApiResponse.ok(scannerService.availableScannerTypes()));
    }
}
