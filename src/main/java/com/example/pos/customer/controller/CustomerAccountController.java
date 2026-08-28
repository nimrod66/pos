package com.example.pos.customer.controller;

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
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID customerId) {
        BigDecimal balance = accountService.getCurrentBalance(customerId);
        Customer customer = accountService.getCustomer(customerId);
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "balance", balance,
                "creditLimit", customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO,
                "accountStatus", customer.getAccountStatus()
        ));
    }

    @GetMapping("/{customerId}/transactions")
    @PreAuthorize("hasAuthority('customer.account.read')")
    public ResponseEntity<List<CustomerTransaction>> getTransactions(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accountService.getTransactionHistory(customerId));
    }

    @PostMapping("/{customerId}/payments")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<Map<String, Object>> recordPayment(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = body.get("description") != null ? body.get("description").toString() : null;
        Customer customer = accountService.recordPayment(customerId, amount, description);
        return ResponseEntity.ok(Map.of(
                "customerId", customer.getId(),
                "newBalance", customer.getBalance(),
                "message", "Payment recorded successfully"
        ));
    }

    @PostMapping("/{customerId}/adjustments")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<Map<String, Object>> adjustBalance(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = body.get("reason").toString();
        Customer customer = accountService.adjustBalance(customerId, amount, reason);
        return ResponseEntity.ok(Map.of(
                "customerId", customer.getId(),
                "newBalance", customer.getBalance(),
                "message", "Balance adjusted successfully"
        ));
    }

    @PutMapping("/{customerId}/credit-limit")
    @PreAuthorize("hasAuthority('customer.account.write')")
    public ResponseEntity<Map<String, Object>> updateCreditLimit(
            @PathVariable UUID customerId,
            @RequestBody Map<String, Object> body) {
        BigDecimal creditLimit = body.get("creditLimit") != null
                ? new BigDecimal(body.get("creditLimit").toString()) : null;
        Customer customer = accountService.updateCreditLimit(customerId, creditLimit);
        return ResponseEntity.ok(Map.of(
                "customerId", customer.getId(),
                "creditLimit", customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO,
                "message", "Credit limit updated successfully"
        ));
    }

    @GetMapping("/{customerId}/outstanding-sales")
    @PreAuthorize("hasAuthority('customer.account.read')")
    public ResponseEntity<List<Sales>> getOutstandingSales(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accountService.getOutstandingSales(customerId));
    }
}
