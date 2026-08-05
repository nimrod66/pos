package com.example.pos.masterdata.categories.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.masterdata.categories.dto.CategoryRequestDto;
import com.example.pos.masterdata.categories.dto.CategoryResponseDto;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.categories.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDto>> create(@RequestBody @Valid CategoryRequestDto dto) {
        MedicineCategories category = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(CategoryResponseDto.from(category)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CategoryResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MedicineCategories> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CategoryResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(CategoryResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid CategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(CategoryResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
