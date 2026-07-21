package com.example.pos.sale.salereturns.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.sale.salereturns.dto.SaleReturnRequestDto;
import com.example.pos.sale.salereturns.dto.SaleReturnResponseDto;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.service.SaleReturnsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sale-returns")
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
    public ResponseEntity<ApiResponse<List<SaleReturnResponseDto>>> getBySale(
            @RequestParam Long saleId) {
        List<SaleReturnResponseDto> list = returnsService.getReturnsBySale(saleId).stream()
                .map(returnsService::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleReturnResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(returnsService.toResponseDto(returnsService.getReturnById(id))));
    }
}
