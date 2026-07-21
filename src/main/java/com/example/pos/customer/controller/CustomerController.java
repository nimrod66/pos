package com.example.pos.customer.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.customer.dto.CustomerRequestDto;
import com.example.pos.customer.dto.CustomerResponseDto;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> create(@RequestBody @Valid CustomerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(CustomerResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(CustomerResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponseDto.from(service.getById(id))));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponseDto.from(service.findByPhone(phone))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> update(@PathVariable Long id, @RequestBody @Valid CustomerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(CustomerResponseDto.from(service.update(id, dto))));
    }

    @PatchMapping("/{id}/loyalty")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> addLoyalty(@PathVariable Long id, @RequestParam int points) {
        return ResponseEntity.ok(ApiResponse.updated(CustomerResponseDto.from(service.addLoyaltyPoints(id, points))));
    }
}
