package com.example.pos.masterdata.units.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.masterdata.units.dto.UnitRequestDto;
import com.example.pos.masterdata.units.dto.UnitResponseDto;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.masterdata.units.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitService service;
    public UnitController(UnitService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<UnitResponseDto>> create(@RequestBody @Valid UnitRequestDto dto) {
        Unit unit = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(UnitResponseDto.from(unit)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnitResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(UnitResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UnitResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(UnitResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UnitResponseDto>> update(@PathVariable Long id, @RequestBody @Valid UnitRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(UnitResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
