package com.example.pos.customer.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.model.CustomerTransaction;
import com.example.pos.customer.service.CustomerAccountService;
import com.example.pos.sale.sales.model.Sales;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerAccountController {

    private final CustomerAccountService accountService;

    @GetMapping("/{customerId}/balance")
    @PreAuthorize("hasAuthority('customer.account.read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(@PathVariable UUID customerId) {
        BigDecimal balance = accountService.getCurrentBalance(customerId);
        Customer customer = accountService.getCustomer(customerId);
        BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of(
                "customerId", customerId,
                "balance", balance,
                "creditLimit", creditLimit,
                "availableCredit", creditLimit.subtract(balance.max(BigDecimal.ZERO)),
                "accountStatus", customer.getAccountStatus() != null ? customer.getAccountStatus() : "ACTIVE"
        )));
    }

    @GetMapping("/{customerId}/transactions")
    @PreAuthorize("hasAuthority('customer.account.read')")
    public ResponseEntity<ApiResponse<List<CustomerTransaction>>> getTransactions(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getTransactionHistory(customerId)));
    }

    @PostMapping("/{customerId}/payments")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordPayment(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = body.get("description") != null ? body.get("description").toString()
                : (body.get("notes") != null ? body.get("notes").toString() : null);
        Customer customer = accountService.recordPayment(customerId, amount, description);
        return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of(
                "customerId", customer.getId(),
                "newBalance", customer.getBalance(),
                "message", "Payment recorded successfully"
        )));
    }

    @PostMapping("/{customerId}/adjustments")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adjustBalance(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString()
                : (body.get("notes") != null ? body.get("notes").toString() : "Manual adjustment");
        Customer customer = accountService.adjustBalance(customerId, amount, reason);
        return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of(
                "customerId", customer.getId(),
                "newBalance", customer.getBalance(),
                "message", "Balance adjusted successfully"
        )));
    }

    @PutMapping("/{customerId}/credit-limit")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCreditLimit(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal creditLimit = body.get("creditLimit") != null
                ? new BigDecimal(body.get("creditLimit").toString()) : null;
        Customer customer = accountService.updateCreditLimit(customerId, creditLimit);
        return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of(
                "customerId", customer.getId(),
                "creditLimit", customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO,
                "message", "Credit limit updated successfully"
        )));
    }

    @GetMapping("/{customerId}/outstanding-sales")
    @PreAuthorize("hasAuthority('customer.account.read')")
    public ResponseEntity<ApiResponse<List<Sales>>> getOutstandingSales(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getOutstandingSales(customerId)));
    }
}
