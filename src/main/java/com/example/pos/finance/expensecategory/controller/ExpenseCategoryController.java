package com.example.pos.finance.expensecategory.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.finance.expensecategory.dto.ExpenseCategoryRequestDto;
import com.example.pos.finance.expensecategory.dto.ExpenseCategoryResponseDto;
import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import com.example.pos.finance.expensecategory.service.ExpenseCategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService service;
    public ExpenseCategoryController(ExpenseCategoryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseCategoryResponseDto>> create(@RequestBody @Valid ExpenseCategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(ExpenseCategoryResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ExpenseCategoryResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ExpenseCategory> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, ExpenseCategoryResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ExpenseCategoryResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseCategoryResponseDto>> update(@PathVariable Long id, @RequestBody @Valid ExpenseCategoryRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(ExpenseCategoryResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.deleted()); }
}
