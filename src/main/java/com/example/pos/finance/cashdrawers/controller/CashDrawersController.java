package com.example.pos.finance.cashdrawers.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.finance.cashdrawers.dto.CashDrawerRequestDto;
import com.example.pos.finance.cashdrawers.dto.CashDrawerResponseDto;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.service.CashDrawersService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cash-drawers")
public class CashDrawersController {

    private final CashDrawersService service;
    public CashDrawersController(CashDrawersService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<CashDrawerResponseDto>> openDrawer(@RequestBody @Valid CashDrawerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(CashDrawerResponseDto.from(service.openDrawer(dto))));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<CashDrawerResponseDto>> closeDrawer(@PathVariable Long id, @RequestBody @Valid CashDrawerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(CashDrawerResponseDto.from(service.closeDrawer(id, dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CashDrawerResponseDto>>> getByShift(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam Long shiftId) {
        Page<CashDrawers> page = service.getByShift(shiftId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CashDrawerResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CashDrawerResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(CashDrawerResponseDto.from(service.getById(id))));
    }
}
