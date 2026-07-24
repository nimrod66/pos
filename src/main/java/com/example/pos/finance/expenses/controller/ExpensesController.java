package com.example.pos.finance.expenses.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.finance.expenses.dto.ExpensesRequestDto;
import com.example.pos.finance.expenses.dto.ExpensesResponseDto;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.finance.expenses.service.ExpensesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpensesController {

    private final ExpensesService service;
    public ExpensesController(ExpensesService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpensesResponseDto>> create(@RequestBody @Valid ExpensesRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(ExpensesResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ExpensesResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Expenses> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, ExpensesResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpensesResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ExpensesResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpensesResponseDto>> update(@PathVariable Long id, @RequestBody @Valid ExpensesRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(ExpensesResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.deleted()); }
}
