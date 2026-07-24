package com.example.pos.compliance.invoice.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.compliance.invoice.dto.CreditNoteResponseDto;
import com.example.pos.compliance.invoice.dto.DebitNoteResponseDto;
import com.example.pos.compliance.invoice.dto.SaleFiscalData;
import com.example.pos.compliance.invoice.dto.TaxInvoiceResponseDto;
import com.example.pos.compliance.invoice.model.*;
import com.example.pos.compliance.invoice.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CreditNoteService creditNoteService;
    private final DebitNoteService debitNoteService;

    public InvoiceController(InvoiceService invoiceService,
                             CreditNoteService creditNoteService,
                             DebitNoteService debitNoteService) {
        this.invoiceService = invoiceService;
        this.creditNoteService = creditNoteService;
        this.debitNoteService = debitNoteService;
    }

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<TaxInvoiceResponseDto>> issue(
            @RequestBody SaleFiscalData saleData,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actorName) {
        TaxInvoice invoice = invoiceService.issueFromSale(saleData, actorId, actorName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(TaxInvoiceResponseDto.from(invoice)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxInvoiceResponseDto>> getById(@PathVariable Long id) {
        TaxInvoice invoice = invoiceService.getById(id);
        List<InvoiceHistory> history = invoiceService.getHistory(id);
        return ResponseEntity.ok(ApiResponse.ok(TaxInvoiceResponseDto.from(invoice, history)));
    }

    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<ApiResponse<TaxInvoiceResponseDto>> getBySaleId(@PathVariable Long saleId) {
        TaxInvoice invoice = invoiceService.getBySaleId(saleId);
        List<InvoiceHistory> history = invoiceService.getHistory(invoice.getId());
        return ResponseEntity.ok(ApiResponse.ok(TaxInvoiceResponseDto.from(invoice, history)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TaxInvoiceResponseDto>>> getByBranch(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam Long branchId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Page<TaxInvoice> page;
        if (from != null && to != null) {
            page = invoiceService.getByBranchAndDate(branchId, from, to, pageable);
        } else {
            List<InvoiceStatus> statuses = status != null
                    ? status.stream().map(InvoiceStatus::valueOf).toList()
                    : null;
            page = invoiceService.getByBranch(branchId, statuses, pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, TaxInvoiceResponseDto::from)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TaxInvoiceResponseDto>> cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Manual cancellation") String reason,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actorName) {
        TaxInvoice invoice = invoiceService.cancel(id, reason, actorId, actorName);
        return ResponseEntity.ok(ApiResponse.ok(TaxInvoiceResponseDto.from(invoice)));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<TaxInvoiceResponseDto.HistoryResponse>>> getHistory(@PathVariable Long id) {
        List<InvoiceHistory> history = invoiceService.getHistory(id);
        var response = history.stream().map(h -> TaxInvoiceResponseDto.HistoryResponse.builder()
                .id(h.getId())
                .historyType(h.getHistoryType() != null ? h.getHistoryType().name() : null)
                .description(h.getDescription())
                .actorId(h.getActorId())
                .actorName(h.getActorName())
                .createdAt(h.getCreatedAt())
                .build()).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/credit-notes")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> createCreditNote(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) BigDecimal taxAmount,
            @RequestParam String reason,
            @RequestParam(required = false) Long createdBy) {
        CreditNote cn = creditNoteService.create(id, amount, taxAmount, reason, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(CreditNoteResponseDto.from(cn)));
    }

    @GetMapping("/{id}/credit-notes")
    public ResponseEntity<ApiResponse<List<CreditNoteResponseDto>>> getCreditNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                creditNoteService.getByInvoiceId(id).stream().map(CreditNoteResponseDto::from).toList()));
    }

    @PostMapping("/{id}/debit-notes")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> createDebitNote(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) BigDecimal taxAmount,
            @RequestParam String reason,
            @RequestParam(required = false) Long createdBy) {
        DebitNote dn = debitNoteService.create(id, amount, taxAmount, reason, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(DebitNoteResponseDto.from(dn)));
    }

    @GetMapping("/{id}/debit-notes")
    public ResponseEntity<ApiResponse<List<DebitNoteResponseDto>>> getDebitNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                debitNoteService.getByInvoiceId(id).stream().map(DebitNoteResponseDto::from).toList()));
    }
}
