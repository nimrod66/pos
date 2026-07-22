package com.example.pos.sale.payment.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import com.example.pos.sale.payment.dto.PaymentRequestDto;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.service.PaymentService;
import com.example.pos.sale.sales.dto.SaleResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentGatewayResponse>> addPayment(@RequestBody @Valid PaymentRequestDto dto) {
        Payment payment = paymentService.addPayment(dto);

        String phone = dto.getPhoneNumber() != null ? dto.getPhoneNumber()
                : (payment.getSales() != null && payment.getSales().getUser() != null
                        ? payment.getSales().getUser().getPhoneNumber() : null);

        PaymentGatewayResponse response = paymentService.processPayment(payment, phone);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response.isSuccess() ? ApiResponse.created(response) : ApiResponse.error(response.getResponseDescription()));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<PaymentGatewayResponse>> processPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String phoneNumber) {
        PaymentGatewayResponse response = paymentService.processPayment(id, phoneNumber);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentGatewayResponse>> queryStatus(@PathVariable Long id) {
        PaymentGatewayResponse response = paymentService.queryStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleResponseDto.PaymentResponse>>> getBySale(@RequestParam Long saleId) {
        List<SaleResponseDto.PaymentResponse> list = paymentService.getPaymentsBySale(saleId).stream()
                .map(p -> SaleResponseDto.PaymentResponse.builder()
                        .id(p.getId())
                        .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .transactionReference(p.getTransactionReference())
                        .paymentStatus(p.getPaymentStatus())
                        .paymentDate(p.getPaymentDate())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<ApiResponse<Void>> mpesaCallback(@RequestBody Map<String, Object> callback) {
        paymentService.handleMpesaCallback(callback);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/paystack/callback")
    public ResponseEntity<ApiResponse<Void>> paystackCallback(@RequestBody Map<String, Object> callback) {
        paymentService.handlePaystackCallback(callback);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/stripe/callback")
    public ResponseEntity<ApiResponse<Void>> stripeCallback(@RequestBody Map<String, Object> payload) {
        paymentService.handleStripeCallback(payload);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentGatewayResponse>> refundPayment(@PathVariable Long id) {
        PaymentGatewayResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
