package com.example.pos.presciptions.dispensary.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.presciptions.dispensary.dto.DispensaryRequestDto;
import com.example.pos.presciptions.dispensary.dto.DispensaryResponseDto;
import com.example.pos.presciptions.dispensary.model.Dispensary;
import com.example.pos.presciptions.dispensary.service.DispensaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispensary")
public class DispensaryController {

    private final DispensaryService service;
    public DispensaryController(DispensaryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<DispensaryResponseDto>> dispense(@RequestBody @Valid DispensaryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(DispensaryResponseDto.from(service.dispense(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DispensaryResponseDto>>> getAll(
            @RequestParam(required = false) Long batchId, @RequestParam(required = false) Long userId) {
        List<Dispensary> list = batchId != null ? service.getByBatch(batchId)
                : userId != null ? service.getByUser(userId) : List.of();
        return ResponseEntity.ok(ApiResponse.ok(list.stream().map(DispensaryResponseDto::from).toList()));
    }
}
