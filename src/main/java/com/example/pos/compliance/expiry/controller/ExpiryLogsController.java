package com.example.pos.compliance.expiry.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.expiry.dto.ExpiryLogRequestDto;
import com.example.pos.compliance.expiry.dto.ExpiryLogResponseDto;
import com.example.pos.compliance.expiry.model.ExpiryLogs;
import com.example.pos.compliance.expiry.service.ExpiryLogsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expiry-logs")
public class ExpiryLogsController {

    private final ExpiryLogsService service;
    public ExpiryLogsController(ExpiryLogsService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpiryLogResponseDto>> log(@RequestBody @Valid ExpiryLogRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ExpiryLogResponseDto.from(service.log(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpiryLogResponseDto>>> getAll(@RequestParam(required = false) Long batchId) {
        List<ExpiryLogs> list = batchId != null ? service.getByBatch(batchId) : service.getAll();
        return ResponseEntity.ok(ApiResponse.ok(list.stream().map(ExpiryLogResponseDto::from).toList()));
    }
}
