package com.example.pos.finance.cashtransactions.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.finance.cashtransactions.dto.CashTransactionResponseDto;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.service.CashTransactionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cash-transactions")
public class CashTransactionsController {

    private final CashTransactionsService service;

    public CashTransactionsController(CashTransactionsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CashTransactionResponseDto>>> getByDrawer(
            @RequestParam Long cashDrawerId) {
        List<CashTransactions> transactions = service.getByCashDrawer(cashDrawerId);
        List<CashTransactionResponseDto> response = transactions.stream()
                .map(CashTransactionResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
