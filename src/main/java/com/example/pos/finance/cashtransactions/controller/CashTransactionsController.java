package com.example.pos.finance.cashtransactions.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.finance.cashtransactions.dto.CashTransactionRequestDto;
import com.example.pos.finance.cashtransactions.dto.CashTransactionResponseDto;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.service.CashTransactionsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cash-transactions")
public class CashTransactionsController {

    private final CashTransactionsService service;

    public CashTransactionsController(CashTransactionsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<PagedResponse<CashTransactionResponseDto>>> getByDrawer(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam UUID cashDrawerId) {
        Page<CashTransactions> page = service.getByCashDrawer(cashDrawerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, CashTransactionResponseDto::from)));
    }

    /** Drawer + recent transactions for the signed-in user's active shift. */
    @GetMapping("/active-drawer")
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<CashTransactionsService.ActiveDrawer>> activeDrawer() {
        return ResponseEntity.ok(ApiResponse.ok(service.activeDrawer()));
    }

    /** Records a deliberate pay-in (CASH_IN) or pay-out (CASH_OUT) mid-shift. */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<CashTransactionResponseDto>> record(
            @RequestBody @Valid CashTransactionRequestDto request) {
        return ResponseEntity.ok(ApiResponse.created(service.recordForActiveShift(request)));
    }
}
