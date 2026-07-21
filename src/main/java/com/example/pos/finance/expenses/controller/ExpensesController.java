package com.example.pos.finance.expenses.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.finance.expenses.dto.ExpensesRequestDto;
import com.example.pos.finance.expenses.dto.ExpensesResponseDto;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.finance.expenses.service.ExpensesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<List<ExpensesResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(ExpensesResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpensesResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ExpensesResponseDto.from(service.getById(id))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.deleted()); }
}
