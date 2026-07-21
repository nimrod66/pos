package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${paystack.secret-key:}')")
public class PaystackPaymentGateway implements PaymentGateway {

    private static final String PAYSTACK_BASE = "https://api.paystack.co";

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.public-key:}")
    private String publicKey;

    @Value("${paystack.callback-url:}")
    private String callbackUrl;

    @Value("${paystack.subaccount:}")
    private String subaccount;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "CARD";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        if (!isConfigured()) {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .responseCode("CONFIG_ERROR")
                    .responseDescription("Paystack not configured. Set paystack.secret-key")
                    .build();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("email", request.getEmail() != null ? request.getEmail() : "customer@pos.local");
            body.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
            body.put("currency", request.getCurrency() != null ? request.getCurrency() : "KES");
            body.put("reference", request.getReference());

            if (request.getCallbackUrl() != null) {
                body.put("callback_url", request.getCallbackUrl());
            } else if (callbackUrl != null && !callbackUrl.isBlank()) {
                body.put("callback_url", callbackUrl);
            }

            if (subaccount != null && !subaccount.isBlank()) {
                body.put("subaccount", subaccount);
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("orderRef", request.getAccountReference());
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }
            body.put("metadata", metadata);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYSTACK_BASE + "/transaction/initialize", entity, Map.class);

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                return fail("Empty response from Paystack");
            }

            boolean status = Boolean.TRUE.equals(respBody.get("status"));
            Map<String, Object> data = (Map<String, Object>) respBody.get("data");

            if (status && data != null) {
                return PaymentGatewayResponse.builder()
                        .success(true)
                        .transactionReference((String) data.get("reference"))
                        .status("PROCESSING")
                        .responseCode("0")
                        .responseDescription((String) data.get("authorization_url"))
                        .merchantRequestId((String) data.get("access_code"))
                        .rawResponse(respBody.toString())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            return fail(String.valueOf(respBody.getOrDefault("message", "Paystack initialization failed")));

        } catch (Exception e) {
            log.error("Paystack payment initialization failed", e);
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .responseCode("PAYSTACK_ERROR")
                    .responseDescription(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse queryStatus(String transactionReference) {
        if (!isConfigured()) {
            return fail("Paystack not configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PAYSTACK_BASE + "/transaction/verify/" + transactionReference,
                    HttpMethod.GET, entity, Map.class);

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                return fail("Empty response from Paystack verify");
            }

            boolean status = Boolean.TRUE.equals(respBody.get("status"));
            Map<String, Object> data = (Map<String, Object>) respBody.get("data");

            if (status && data != null) {
                String paystackStatus = (String) data.get("status");
                String mappedStatus = mapPaystackStatus(paystackStatus);
                return PaymentGatewayResponse.builder()
                        .success("success".equals(paystackStatus))
                        .transactionReference(transactionReference)
                        .status(mappedStatus)
                        .responseCode("0")
                        .responseDescription("Payment " + mappedStatus)
                        .rawResponse(respBody.toString())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            return fail("Payment not found or verification failed");

        } catch (Exception e) {
            log.error("Paystack verification failed: {}", e.getMessage());
            return fail("Verification error: " + e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        if (!isConfigured()) {
            return fail("Paystack not configured");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transaction", transactionReference);
            if (amount != null) {
                body.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYSTACK_BASE + "/refund", entity, Map.class);

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                return fail("Empty response from Paystack refund");
            }

            boolean status = Boolean.TRUE.equals(respBody.get("status"));
            return PaymentGatewayResponse.builder()
                    .success(status)
                    .transactionReference("REF-" + transactionReference)
                    .status(status ? "REFUNDED" : "FAILED")
                    .responseCode(status ? "0" : "REFUND_FAILED")
                    .responseDescription(String.valueOf(respBody.getOrDefault("message", "Refund processed")))
                    .rawResponse(respBody.toString())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Paystack refund failed", e);
            return fail("Refund error: " + e.getMessage());
        }
    }

    private boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    private String mapPaystackStatus(String paystackStatus) {
        return switch (paystackStatus.toLowerCase()) {
            case "success" -> "COMPLETED";
            case "abandoned", "failed" -> "FAILED";
            case "reversed" -> "REFUNDED";
            default -> "PENDING";
        };
    }

    private PaymentGatewayResponse fail(String message) {
        return PaymentGatewayResponse.builder()
                .success(false)
                .status("FAILED")
                .responseDescription(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
