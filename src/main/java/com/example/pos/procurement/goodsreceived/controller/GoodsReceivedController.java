package com.example.pos.procurement.goodsreceived.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedResponseDto;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.goodsreceived.service.GoodsReceivedNotesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goods-received")
public class GoodsReceivedController {

    private final GoodsReceivedNotesService service;
    public GoodsReceivedController(GoodsReceivedNotesService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<GoodsReceivedResponseDto>> receive(@RequestBody @Valid GoodsReceivedRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(GoodsReceivedResponseDto.from(service.receive(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<GoodsReceivedResponseDto>>> getByPO(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam UUID poId) {
        Page<GoodsReceivedNotes> page = service.getByPurchaseOrder(poId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, GoodsReceivedResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoodsReceivedResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(GoodsReceivedResponseDto.from(service.getById(id))));
    }
}
