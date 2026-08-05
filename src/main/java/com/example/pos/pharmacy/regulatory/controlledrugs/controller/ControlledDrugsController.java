package com.example.pos.pharmacy.regulatory.controlledrugs.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.pharmacy.regulatory.controlledrugs.dto.ControlledDrugsRequestDto;
import com.example.pos.pharmacy.regulatory.controlledrugs.dto.ControlledDrugsResponseDto;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.pharmacy.regulatory.controlledrugs.service.ControlledDrugsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/controlled-drugs")
public class ControlledDrugsController {

    private final ControlledDrugsService service;
    public ControlledDrugsController(ControlledDrugsService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ControlledDrugsResponseDto>> record(@RequestBody @Valid ControlledDrugsRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ControlledDrugsResponseDto.from(service.record(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ControlledDrugsResponseDto>>> getAll(
            @RequestParam(required = false) UUID medicineId) {
        List<ControlledDrugs> list = medicineId != null ? service.getByMedicine(medicineId) : service.getAll();
        return ResponseEntity.ok(ApiResponse.ok(list.stream().map(ControlledDrugsResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ControlledDrugsResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ControlledDrugsResponseDto.from(service.getById(id))));
    }
}
