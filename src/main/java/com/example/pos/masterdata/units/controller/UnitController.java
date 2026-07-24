package com.example.pos.masterdata.units.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.masterdata.units.dto.UnitRequestDto;
import com.example.pos.masterdata.units.dto.UnitResponseDto;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.masterdata.units.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Unit> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, UnitResponseDto::from)));
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
