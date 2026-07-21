package com.example.pos.procurement.goodsreceived.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedResponseDto;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.goodsreceived.service.GoodsReceivedNotesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-received")
public class GoodsReceivedController {

    private final GoodsReceivedNotesService service;
    public GoodsReceivedController(GoodsReceivedNotesService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<GoodsReceivedResponseDto>> receive(@RequestBody @Valid GoodsReceivedRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(GoodsReceivedResponseDto.from(service.receive(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoodsReceivedResponseDto>>> getByPO(@RequestParam Long poId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByPurchaseOrder(poId).stream().map(GoodsReceivedResponseDto::from).toList()));
    }
}
