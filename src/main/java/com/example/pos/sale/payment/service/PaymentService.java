package com.example.pos.sale.payment.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayFactory;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import com.example.pos.sale.payment.dto.PaymentRequestDto;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class PaymentService {

    private static final Set<String> PROCESSED_CALLBACKS = ConcurrentHashMap.newKeySet();
    private static final int MAX_PROCESSED_CALLBACKS = 10000;

    private final PaymentRepository paymentRepository;
    private final SalesRepository salesRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final SyncService syncService;
    private final TerminalConfig terminalConfig;

    @Value("${mpesa.callback-hmac-key:}")
    private String callbackHmacKey;

    @Value("${mpesa.callback-replay-window-seconds:300}")
    private int replayWindowSeconds;

    public PaymentService(PaymentRepository paymentRepository,
                          SalesRepository salesRepository,
                          PaymentGatewayFactory gatewayFactory,
                          SyncService syncService,
                          TerminalConfig terminalConfig) {
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
        this.gatewayFactory = gatewayFactory;
        this.syncService = syncService;
        this.terminalConfig = terminalConfig;
    }

    public Payment addPayment(PaymentRequestDto dto) {
        Sales sale = salesRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", dto.getSaleId()));

        Payment payment = new Payment();
        payment.setSales(sale);
        try {
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment method: " + dto.getPaymentMethod()
                    + ". Valid: " + gatewayFactory.listConfigured().keySet());
        }
        payment.setAmount(dto.getAmount());
        payment.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "KES");
        payment.setDescription(dto.getDescription());
        payment.setPaymentDate(LocalDateTime.now());

        if (dto.getPaymentMethod().equalsIgnoreCase("CASH")) {
            payment.setTransactionReference("CASH-" + System.currentTimeMillis());
            payment.setPaymentStatus("COMPLETED");
        } else {
            payment.setPaymentStatus("PENDING");
        }

        payment = paymentRepository.save(payment);
        recalculateSalePaymentStatus(sale);

        return payment;
    }

    public PaymentGatewayResponse processPayment(Payment payment, String phoneNumber) {
        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());

        String email = payment.getSales() != null && payment.getSales().getCustomer() != null
                ? payment.getSales().getCustomer().getEmail()
                : (payment.getSales() != null && payment.getSales().getUser() != null
                        ? payment.getSales().getUser().getEmail() : null);

        PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                .phoneNumber(phoneNumber)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .reference("SALE-" + payment.getSales().getId() + "-PMT-" + payment.getId())
                .accountReference(payment.getSales().getInvoiceNumber())
                .description(payment.getDescription())
                .email(email)
                .build();

        PaymentGatewayResponse response = gateway.process(request);
        log.info("Payment processed: method={}, success={}, ref={}",
                payment.getPaymentMethod(), response.isSuccess(), response.getTransactionReference());

        if (response.isSuccess()) {
            payment.setTransactionReference(response.getTransactionReference());
            payment.setPaymentStatus(response.getStatus());
            paymentRepository.save(payment);

            if ("COMPLETED".equalsIgnoreCase(response.getStatus())) {
                recalculateSalePaymentStatus(payment.getSales());
            }
        } else {
            payment.setPaymentStatus("FAILED");
            payment.setTransactionReference(response.getResponseCode());
            paymentRepository.save(payment);
        }

        return response;
    }

    public PaymentGatewayResponse processPayment(Long paymentId, String phoneNumber) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return processPayment(payment, phoneNumber);
    }

    public PaymentGatewayResponse queryStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getTransactionReference() == null) {
            throw new BadRequestException("Payment has no transaction reference to query");
        }

        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
        PaymentGatewayResponse response = gateway.queryStatus(payment.getTransactionReference());

        if (response.isSuccess() && "COMPLETED".equalsIgnoreCase(response.getStatus())) {
            payment.setPaymentStatus("COMPLETED");
            paymentRepository.save(payment);
            recalculateSalePaymentStatus(payment.getSales());
        }

        return response;
    }

    public void handleMpesaCallback(Map<String, Object> callback) {
        log.info("M-Pesa callback received: {}", callback);
        Map<String, Object> stkCallback = (Map<String, Object>) ((Map<String, Object>)
                callback.getOrDefault("Body", Map.of())).get("stkCallback");

        if (stkCallback == null) return;

        String resultCode = String.valueOf(stkCallback.get("ResultCode"));
        String merchantRequestId = (String) stkCallback.get("MerchantRequestID");
        String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");

        String callbackSignature = (String) callback.get("X-Signature");
        if (callbackHmacKey != null && !callbackHmacKey.isEmpty()) {
            if (!validateHmac(callback, callbackSignature)) {
                log.warn("M-Pesa callback HMAC validation FAILED for {}", merchantRequestId);
                throw new RuntimeException("Invalid callback signature");
            }
        }

        String callbackKey = merchantRequestId + ":" + checkoutRequestId;
        if (!PROCESSED_CALLBACKS.add(callbackKey)) {
            log.info("M-Pesa callback already processed: {}", callbackKey);
            return;
        }

        if (PROCESSED_CALLBACKS.size() > MAX_PROCESSED_CALLBACKS) {
            log.warn("M-Pesa callback set is large ({}), clearing old entries", PROCESSED_CALLBACKS.size());
            PROCESSED_CALLBACKS.clear();
        }

        Payment payment = paymentRepository.findByTransactionReference(merchantRequestId)
                .orElse(null);

        if (payment == null) {
            log.warn("No payment found for MPesa MerchantRequestID: {}", merchantRequestId);
            return;
        }

        if ("COMPLETED".equals(payment.getPaymentStatus()) || "FAILED".equals(payment.getPaymentStatus())) {
            log.info("Payment {} already finalized, ignoring callback", payment.getId());
            return;
        }

        if ("0".equals(resultCode)) {
            payment.setPaymentStatus("COMPLETED");
            payment = paymentRepository.save(payment);
            recalculateSalePaymentStatus(payment.getSales());

            syncService.writeOutboxEvent(EventType.PAYMENT_RECEIVED, "PAYMENT",
                    payment.getId().toString(),
                    "{\"amount\":" + payment.getAmount()
                            + ",\"method\":\"MPESA\""
                            + ",\"ref\":\"" + merchantRequestId + "\""
                            + ",\"saleId\":" + payment.getSales().getId()
                            + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");
        } else {
            payment.setPaymentStatus("FAILED");
            payment.setDescription("M-Pesa failed: " + stkCallback.get("ResultDesc"));
        }
        paymentRepository.save(payment);
    }

    private boolean validateHmac(Map<String, Object> callback, String providedSignature) {
        if (providedSignature == null) return false;
        try {
            String body = callback.toString();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(callbackHmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hash);
            return computed.equals(providedSignature);
        } catch (Exception e) {
            log.error("HMAC validation failed", e);
            return false;
        }
    }

    public PaymentGatewayResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!"COMPLETED".equals(payment.getPaymentStatus())) {
            throw new BadRequestException("Only completed payments can be refunded");
        }

        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
        PaymentGatewayResponse response = gateway.refund(
                payment.getTransactionReference(), payment.getAmount());

        if (response.isSuccess()) {
            payment.setPaymentStatus("REFUNDED");
            payment.setDescription("Refunded: " + response.getResponseDescription());
            paymentRepository.save(payment);
            recalculateSalePaymentStatus(payment.getSales());
        }

        return response;
    }

    public void handlePaystackCallback(Map<String, Object> callback) {
        log.info("Paystack callback received: {}", callback);

        String event = (String) callback.get("event");
        Map<String, Object> data = (Map<String, Object>) callback.get("data");
        if (data == null) return;

        String reference = (String) data.get("reference");
        if (reference == null) return;

        Payment payment = paymentRepository.findByTransactionReference(reference).orElse(null);
        if (payment == null) {
            log.warn("No payment found for Paystack reference: {}", reference);
            return;
        }

        if ("COMPLETED".equals(payment.getPaymentStatus())
                || "FAILED".equals(payment.getPaymentStatus())) {
            log.info("Paystack callback: payment {} already finalized", payment.getId());
            return;
        }

        String paystackStatus = (String) data.get("status");

        if ("charge.success".equals(event) && "success".equalsIgnoreCase(paystackStatus)) {
            payment.setPaymentStatus("COMPLETED");
            recalculateSalePaymentStatus(payment.getSales());

            syncService.writeOutboxEvent(EventType.PAYMENT_RECEIVED, "PAYMENT",
                    payment.getId().toString(),
                    "{\"amount\":" + payment.getAmount()
                            + ",\"method\":\"CARD\""
                            + ",\"ref\":\"" + reference + "\""
                            + ",\"saleId\":" + payment.getSales().getId()
                            + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");
        } else {
            payment.setPaymentStatus("FAILED");
            payment.setDescription("Paystack: " + event);
        }
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsBySale(Long saleId) {
        return paymentRepository.findBySalesId(saleId);
    }

    private void recalculateSalePaymentStatus(Sales sale) {
        BigDecimal totalPaid = paymentRepository.findBySalesId(sale.getId()).stream()
                .filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(sale.getTotal()) >= 0) {
            sale.setPaymentStatus(Sales.PaymentStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus(Sales.PaymentStatus.IN_PROGRESS);
        } else {
            sale.setPaymentStatus(Sales.PaymentStatus.NOT_PAID);
        }
        salesRepository.save(sale);
    }
}
