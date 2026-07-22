package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${stripe.secret-key:}')")
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.base-url:https://api.stripe.com}")
    private String baseUrl;

    @Override
    public String getType() {
        return "STRIPE";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        if (secretKey == null || secretKey.isBlank()) {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("CONFIG_ERROR")
                    .responseDescription("Stripe secret key not configured")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        try {
            String idempotencyKey = UUID.randomUUID().toString();
            long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", amountInCents);
            body.put("currency", request.getCurrency() != null ? request.getCurrency().toLowerCase() : "kes");
            body.put("description", request.getDescription() != null ? request.getDescription() : "POS Payment");
            if (request.getEmail() != null) {
                body.put("receipt_email", request.getEmail());
            }

            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("reference", request.getReference());
            body.put("metadata", metadata);

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey, "");
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Idempotency-Key", idempotencyKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/payment_intents",
                    HttpMethod.POST, entity, Map.class);

            String paymentIntentId = response.getBody() != null
                    ? (String) response.getBody().get("id") : null;
            String clientSecret = response.getBody() != null
                    ? (String) response.getBody().get("client_secret") : null;

            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionReference(paymentIntentId)
                    .status(PaymentGatewayResponse.Status.PROCESSING.name())
                    .responseCode("pi_created")
                    .responseDescription(clientSecret)
                    .rawResponse(response.getBody() != null ? response.getBody().toString() : null)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Stripe payment intent creation failed: {}", e.getMessage());
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .transactionReference(request.getReference())
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("STRIPE_ERROR")
                    .responseDescription(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse queryStatus(String transactionReference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey, "");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/payment_intents/" + transactionReference,
                    HttpMethod.GET, entity, Map.class);

            String status = response.getBody() != null
                    ? (String) response.getBody().get("status") : null;

            return PaymentGatewayResponse.builder()
                    .success("succeeded".equals(status))
                    .transactionReference(transactionReference)
                    .status(mapStripeStatus(status))
                    .responseCode(status)
                    .rawResponse(response.getBody() != null ? response.getBody().toString() : null)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Stripe query failed: {}", e.getMessage());
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .transactionReference(transactionReference)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("QUERY_ERROR")
                    .responseDescription(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("payment_intent", transactionReference);
            body.put("amount", amountInCents);

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey, "");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v1/refunds",
                    HttpMethod.POST, entity, Map.class);

            String refundId = response.getBody() != null
                    ? (String) response.getBody().get("id") : null;

            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionReference(refundId)
                    .status(PaymentGatewayResponse.Status.REFUNDED.name())
                    .responseCode("refund_succeeded")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .transactionReference(transactionReference)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("REFUND_ERROR")
                    .responseDescription(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private String mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return PaymentGatewayResponse.Status.PENDING.name();
        return switch (stripeStatus) {
            case "succeeded" -> PaymentGatewayResponse.Status.COMPLETED.name();
            case "requires_payment_method", "requires_confirmation", "requires_action", "processing" ->
                    PaymentGatewayResponse.Status.PROCESSING.name();
            case "canceled" -> PaymentGatewayResponse.Status.CANCELLED.name();
            default -> PaymentGatewayResponse.Status.FAILED.name();
        };
    }
}
