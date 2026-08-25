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
import com.example.pos.sale.sales.service.SaleService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AuthenticatedUserContext current;
    private final SaleService saleService;
    private final MpesaSettings mpesaSettings;

    @Value("${mpesa.callback-hmac-key:}")
    private String callbackHmacKey;

    @Value("${mpesa.callback-replay-window-seconds:300}")
    private int replayWindowSeconds;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          SalesRepository salesRepository,
                          PaymentGatewayFactory gatewayFactory,
                          SyncService syncService,
                          TerminalConfig terminalConfig,
                          AuthenticatedUserContext current,
                          SaleService saleService,
                          MpesaSettings mpesaSettings) {
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
        this.gatewayFactory = gatewayFactory;
        this.syncService = syncService;
        this.terminalConfig = terminalConfig;
        this.current = current;
        this.saleService = saleService;
        this.mpesaSettings = mpesaSettings;
    }

    public Payment addPayment(PaymentRequestDto dto) {
        throw new BadRequestException(
                "Payments must be submitted as part of authoritative checkout",
                "DIRECT_PAYMENT_DISABLED");
    }

    public PaymentGatewayResponse processPayment(Payment payment, String phoneNumber) {
        if (payment.getPaymentMethod() != Payment.PaymentMethod.M_PESA) {
            throw new BadRequestException("Only pending M-Pesa STK payments can be processed",
                    "PAYMENT_NOT_PROCESSABLE");
        }
        if ("COMPLETED".equalsIgnoreCase(payment.getPaymentStatus())) {
            return currentResponse(payment, PaymentGatewayResponse.Status.COMPLETED);
        }
        if ("FAILED".equalsIgnoreCase(payment.getPaymentStatus())
                || "CANCELLED".equalsIgnoreCase(payment.getPaymentStatus())) {
            return currentResponse(payment, PaymentGatewayResponse.Status.valueOf(
                    payment.getPaymentStatus().toUpperCase(Locale.ROOT)));
        }
        if ("UNKNOWN".equalsIgnoreCase(payment.getPaymentStatus())) {
            return currentResponse(payment, PaymentGatewayResponse.Status.PENDING);
        }
        if (payment.getCheckoutRequestId() != null) {
            return currentResponse(payment, PaymentGatewayResponse.Status.PROCESSING);
        }

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
            payment.setMerchantRequestId(response.getMerchantRequestId());
            payment.setCheckoutRequestId(response.getCheckoutRequestId());
            payment.setPaymentStatus(response.getStatus());
            paymentRepository.saveAndFlush(payment);

            if ("COMPLETED".equalsIgnoreCase(response.getStatus())) {
                payment.setPaymentDate(LocalDateTime.now());
                saleService.finalizeOnlinePayment(payment.getSales().getId());
            }
        } else {
            boolean uncertain = "PROCESSING_ERROR".equalsIgnoreCase(response.getResponseCode());
            payment.setPaymentStatus(uncertain ? "UNKNOWN" : "FAILED");
            payment.setDescription(response.getResponseDescription());
            paymentRepository.saveAndFlush(payment);
            if (!uncertain) saleService.failOnlinePayment(payment.getSales().getId());
            if (uncertain) {
                response.setStatus(PaymentGatewayResponse.Status.PENDING.name());
            }
        }

        return response;
    }

    public PaymentGatewayResponse processPayment(UUID paymentId, String phoneNumber) {
        Payment payment = paymentRepository.findByIdAndSalesBranchId(paymentId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return processPayment(payment, phoneNumber);
    }

    public PaymentGatewayResponse queryStatus(UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndSalesBranchId(paymentId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if ("COMPLETED".equalsIgnoreCase(payment.getPaymentStatus())) {
            return currentResponse(payment, PaymentGatewayResponse.Status.COMPLETED);
        }
        if ("FAILED".equalsIgnoreCase(payment.getPaymentStatus())
                || "CANCELLED".equalsIgnoreCase(payment.getPaymentStatus())) {
            return currentResponse(payment, PaymentGatewayResponse.Status.valueOf(
                    payment.getPaymentStatus().toUpperCase(Locale.ROOT)));
        }
        if (payment.getCheckoutRequestId() == null) {
            throw new BadRequestException("Payment has no checkout request ID to query",
                    "MPESA_CHECKOUT_ID_MISSING");
        }

        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
        PaymentGatewayResponse response = gateway.queryStatus(payment.getCheckoutRequestId());

        if ("COMPLETED".equalsIgnoreCase(response.getStatus())) {
            payment.setPaymentStatus("COMPLETED");
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.saveAndFlush(payment);
            saleService.finalizeOnlinePayment(payment.getSales().getId());
        } else if ("FAILED".equalsIgnoreCase(response.getStatus())
                || "CANCELLED".equalsIgnoreCase(response.getStatus())) {
            payment.setPaymentStatus(response.getStatus().toUpperCase(Locale.ROOT));
            payment.setDescription(response.getResponseDescription());
            paymentRepository.saveAndFlush(payment);
            saleService.failOnlinePayment(payment.getSales().getId());
        } else {
            payment.setPaymentStatus("PROCESSING");
            paymentRepository.save(payment);
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

        Payment payment = paymentRepository.findByMerchantRequestId(merchantRequestId)
                .or(() -> paymentRepository.findByCheckoutRequestId(checkoutRequestId))
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
            payment.setPaymentDate(LocalDateTime.now());
            payment.setTransactionReference(mpesaReceiptNumber(stkCallback));
            payment = paymentRepository.saveAndFlush(payment);
            saleService.finalizeOnlinePayment(payment.getSales().getId());

            syncService.writeOutboxEvent(EventType.PAYMENT_RECEIVED, "PAYMENT",
                    payment.getId().toString(),
                    "{\"amount\":" + payment.getAmount()
                            + ",\"method\":\"MPESA\""
                            + ",\"ref\":\"" + merchantRequestId + "\""
                            + ",\"saleId\":" + payment.getSales().getId()
                            + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");
        } else {
            payment.setPaymentStatus("1032".equals(resultCode) ? "CANCELLED" : "FAILED");
            payment.setDescription("M-Pesa failed: " + stkCallback.get("ResultDesc"));
            paymentRepository.saveAndFlush(payment);
            saleService.failOnlinePayment(payment.getSales().getId());
        }
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

    public PaymentGatewayResponse refundPayment(UUID paymentId) {
        throw new BadRequestException(
                "Refunds must be recorded through the sale return workflow",
                "DIRECT_REFUND_DISABLED");
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

    public void handleStripeCallback(Map<String, Object> event) {
        log.info("Stripe webhook received");

        String eventType = (String) event.get("type");
        Map<String, Object> dataObj = (Map<String, Object>) event.get("data");
        if (dataObj == null) return;
        Map<String, Object> object = (Map<String, Object>) dataObj.get("object");
        if (object == null) return;

        String paymentIntentId = (String) object.get("id");
        if (paymentIntentId == null) return;

        if (!PROCESSED_CALLBACKS.add("STRIPE:" + paymentIntentId)) {
            log.info("Stripe callback already processed: {}", paymentIntentId);
            return;
        }

        if (PROCESSED_CALLBACKS.size() > MAX_PROCESSED_CALLBACKS) {
            PROCESSED_CALLBACKS.clear();
        }

        Payment payment = paymentRepository.findByTransactionReference(paymentIntentId)
                .orElse(null);
        if (payment == null) {
            log.warn("No payment found for Stripe PaymentIntent: {}", paymentIntentId);
            return;
        }

        if ("COMPLETED".equals(payment.getPaymentStatus())
                || "FAILED".equals(payment.getPaymentStatus())) {
            log.info("Stripe callback: payment {} already finalized", payment.getId());
            return;
        }

        if ("payment_intent.succeeded".equals(eventType)) {
            payment.setPaymentStatus("COMPLETED");
            recalculateSalePaymentStatus(payment.getSales());

            syncService.writeOutboxEvent(EventType.PAYMENT_RECEIVED, "PAYMENT",
                    payment.getId().toString(),
                    "{\"amount\":" + payment.getAmount()
                            + ",\"method\":\"STRIPE\""
                            + ",\"ref\":\"" + paymentIntentId + "\""
                            + ",\"saleId\":" + payment.getSales().getId()
                            + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            payment.setPaymentStatus("FAILED");
            payment.setDescription("Stripe: " + eventType);
        }

        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsBySale(UUID saleId, Pageable pageable) {
        salesRepository.findDetailedByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return paymentRepository.findBySalesIdAndSalesBranchId(
                saleId, current.branchId(), pageable);
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

    @Transactional(readOnly = true)
    public Map<String, Object> capabilities() {
        MpesaSettings.Config cfg = mpesaSettings.resolve();
        return Map.of(
                "mpesaStkConfigured", cfg.stkReady(),
                "mpesaEnvironment", cfg.environment(),
                "pollingSupported", true);
    }

    private PaymentGatewayResponse currentResponse(
            Payment payment, PaymentGatewayResponse.Status status) {
        return PaymentGatewayResponse.builder()
                .success(status == PaymentGatewayResponse.Status.COMPLETED
                        || status == PaymentGatewayResponse.Status.PROCESSING)
                .status(status.name())
                .transactionReference(payment.getTransactionReference())
                .merchantRequestId(payment.getMerchantRequestId())
                .checkoutRequestId(payment.getCheckoutRequestId())
                .responseDescription(payment.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private String mpesaReceiptNumber(Map<String, Object> stkCallback) {
        Map<String, Object> metadata = (Map<String, Object>) stkCallback.get("CallbackMetadata");
        if (metadata == null || !(metadata.get("Item") instanceof List<?> items)) return null;
        for (Object value : items) {
            if (!(value instanceof Map<?, ?> item)) continue;
            if ("MpesaReceiptNumber".equals(item.get("Name")) && item.get("Value") != null) {
                return String.valueOf(item.get("Value"));
            }
        }
        return null;
    }
}
