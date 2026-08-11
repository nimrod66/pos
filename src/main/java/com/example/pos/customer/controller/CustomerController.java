package com.example.pos.customer.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.customer.dto.CustomerRequestDto;
import com.example.pos.customer.dto.CustomerResponseDto;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> create(@RequestBody @Valid CustomerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(CustomerResponseDto.from(service.create(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Customer> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CustomerResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponseDto.from(service.getById(id))));
    }

    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(ApiResponse.ok(CustomerResponseDto.from(service.findByPhone(phone))));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponseDto>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Customer> page = service.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CustomerResponseDto::from)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('pos.sell', 'sale.read')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid CustomerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(CustomerResponseDto.from(service.update(id, dto))));
    }

    @PatchMapping("/{id}/loyalty")
    @PreAuthorize("hasAuthority('settings.manage')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> addLoyalty(@PathVariable UUID id, @RequestParam int points) {
        return ResponseEntity.ok(ApiResponse.updated(CustomerResponseDto.from(service.addLoyaltyPoints(id, points))));
    }
}
