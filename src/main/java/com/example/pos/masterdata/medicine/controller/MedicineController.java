package com.example.pos.masterdata.medicine.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.masterdata.medicine.dto.MedicineRequestDto;
import com.example.pos.masterdata.medicine.dto.MedicineResponseDto;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicineResponseDto>> create(
            @RequestBody @Valid MedicineRequestDto dto) {
        Medicine medicine = medicineService.createMedicine(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MedicineResponseDto.from(medicine)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineResponseDto>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) Boolean controlled) {
        List<Medicine> medicines;
        if (controlled != null && controlled) {
            medicines = medicineService.getControlledDrugs();
        } else if (categoryId != null) {
            medicines = medicineService.getMedicinesByCategory(categoryId);
        } else if (manufacturerId != null) {
            medicines = medicineService.getMedicinesByManufacturer(manufacturerId);
        } else {
            medicines = medicineService.getAllMedicines();
        }
        List<MedicineResponseDto> response = medicines.stream()
                .map(MedicineResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> getByBarcode(
            @PathVariable String barcode) {
        Medicine medicine = medicineService.getMedicineByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.ok(MedicineResponseDto.from(medicine)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> getById(@PathVariable Long id) {
        Medicine medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(ApiResponse.ok(MedicineResponseDto.from(medicine)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid MedicineRequestDto dto) {
        Medicine medicine = medicineService.updateMedicine(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(MedicineResponseDto.from(medicine)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
