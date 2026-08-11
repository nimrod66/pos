package com.example.pos.masterdata.medicine.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.masterdata.medicine.dto.MedicineRequestDto;
import com.example.pos.masterdata.medicine.dto.MedicineResponseDto;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('medicine.write') and hasAuthority('medicine.price.write')")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> create(
            @RequestBody @Valid MedicineRequestDto dto) {
        Medicine medicine = medicineService.createMedicine(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MedicineResponseDto.from(medicine)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('medicine.read')")
    public ResponseEntity<ApiResponse<PagedResponse<MedicineResponseDto>>> getAll(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID manufacturerId,
            @RequestParam(required = false) Boolean controlled,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Medicine> page;
        if (controlled != null && controlled) {
            page = medicineService.getControlledDrugs(pageable);
        } else if (categoryId != null) {
            page = medicineService.getMedicinesByCategory(categoryId, pageable);
        } else if (manufacturerId != null) {
            page = medicineService.getMedicinesByManufacturer(manufacturerId, pageable);
        } else {
            page = medicineService.getAllMedicines(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, MedicineResponseDto::from)));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAuthority('medicine.read')")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> getByBarcode(
            @PathVariable String barcode) {
        Medicine medicine = medicineService.getMedicineByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.ok(MedicineResponseDto.from(medicine)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('medicine.read')")
    public ResponseEntity<ApiResponse<PagedResponse<MedicineResponseDto>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Medicine> page = medicineService.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, MedicineResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('medicine.read')")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> getById(@PathVariable UUID id) {
        Medicine medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(ApiResponse.ok(MedicineResponseDto.from(medicine)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('medicine.write') and hasAuthority('medicine.price.write')")
    public ResponseEntity<ApiResponse<MedicineResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid MedicineRequestDto dto) {
        Medicine medicine = medicineService.updateMedicine(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(MedicineResponseDto.from(medicine)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('medicine.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
