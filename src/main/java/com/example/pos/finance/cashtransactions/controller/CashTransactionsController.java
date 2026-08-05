package com.example.pos.finance.cashtransactions.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.finance.cashtransactions.dto.CashTransactionResponseDto;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.service.CashTransactionsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cash-transactions")
public class CashTransactionsController {

    private final CashTransactionsService service;

    public CashTransactionsController(CashTransactionsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CashTransactionResponseDto>>> getByDrawer(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam UUID cashDrawerId) {
        Page<CashTransactions> page = service.getByCashDrawer(cashDrawerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CashTransactionResponseDto::from)));
    }
}
