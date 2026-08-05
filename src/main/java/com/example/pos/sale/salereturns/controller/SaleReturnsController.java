package com.example.pos.sale.salereturns.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.sale.salereturns.dto.SaleReturnRequestDto;
import com.example.pos.sale.salereturns.dto.SaleReturnResponseDto;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.service.SaleReturnsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sale-returns")
public class SaleReturnsController {

    private final SaleReturnsService returnsService;

    public SaleReturnsController(SaleReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleReturnResponseDto>> create(
            @RequestBody @Valid SaleReturnRequestDto dto) {
        SaleReturns sr = returnsService.createReturn(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(returnsService.toResponseDto(sr)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SaleReturnResponseDto>>> getBySale(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam UUID saleId) {
        Page<SaleReturns> page = returnsService.getReturnsBySale(saleId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, returnsService::toResponseDto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleReturnResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(returnsService.toResponseDto(returnsService.getReturnById(id))));
    }
}
