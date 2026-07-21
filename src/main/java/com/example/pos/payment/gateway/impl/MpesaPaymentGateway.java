package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MpesaPaymentGateway implements PaymentGateway {

    @Value("${mpesa.consumer-key:}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret:}")
    private String consumerSecret;

    @Value("${mpesa.passkey:}")
    private String passkey;

    @Value("${mpesa.shortcode:174379}")
    private String shortcode;

    @Value("${mpesa.environment:sandbox}")
    private String environment;

    @Value("${mpesa.callback-url:}")
    private String callbackUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "M_PESA";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        if (!isConfigured()) {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("CONFIG_ERROR")
                    .responseDescription("M-Pesa not configured. Set mpesa.consumer-key and mpesa.consumer-secret")
                    .build();
        }

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return failResponse("AUTH_ERROR", "Failed to get M-Pesa access token");
            }

            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                    (shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", shortcode);
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("TransactionType", "CustomerPayBillOnline");
            body.put("Amount", request.getAmount().intValue());
            body.put("PartyA", sanitizePhone(request.getPhoneNumber()));
            body.put("PartyB", shortcode);
            body.put("PhoneNumber", sanitizePhone(request.getPhoneNumber()));
            body.put("CallBackURL", callbackUrl);
            body.put("AccountReference", request.getAccountReference() != null
                    ? request.getAccountReference() : request.getReference());
            body.put("TransactionDesc", request.getDescription() != null
                    ? request.getDescription() : "Pharmacy Purchase");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = getBaseUrl() + "/mpesa/stkpush/v1/processrequest";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> respBody = response.getBody();
            String responseCode = String.valueOf(respBody != null ? respBody.get("ResponseCode") : "UNKNOWN");

            if ("0".equals(responseCode)) {
                return PaymentGatewayResponse.builder()
                        .success(true)
                        .transactionReference((String) respBody.get("MerchantRequestID"))
                        .merchantRequestId((String) respBody.get("MerchantRequestID"))
                        .checkoutRequestId((String) respBody.get("CheckoutRequestID"))
                        .status(PaymentGatewayResponse.Status.PROCESSING.name())
                        .responseCode(responseCode)
                        .responseDescription((String) respBody.get("ResponseDescription"))
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                return failResponse(responseCode,
                        respBody != null ? String.valueOf(respBody.get("ResponseDescription")) : "STK Push failed");
            }
        } catch (Exception e) {
            log.error("M-Pesa STK Push error", e);
            return failResponse("PROCESSING_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse queryStatus(String checkoutRequestId) {
        if (!isConfigured()) {
            return failResponse("CONFIG_ERROR", "M-Pesa not configured");
        }
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return failResponse("AUTH_ERROR", "Failed to get access token");
            }

            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                    (shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", shortcode);
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = getBaseUrl() + "/mpesa/stkpushquery/v1/query";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> respBody = response.getBody();
            String resultCode = String.valueOf(respBody != null ? respBody.get("ResultCode") : "UNKNOWN");

            String status = "0".equals(resultCode)
                    ? PaymentGatewayResponse.Status.COMPLETED.name()
                    : "1032".equals(resultCode)
                            ? PaymentGatewayResponse.Status.PROCESSING.name()
                            : PaymentGatewayResponse.Status.FAILED.name();

            return PaymentGatewayResponse.builder()
                    .success("0".equals(resultCode))
                    .status(status)
                    .responseCode(resultCode)
                    .responseDescription((String) respBody.get("ResultDesc"))
                    .checkoutRequestId(checkoutRequestId)
                    .rawResponse(respBody != null ? respBody.toString() : null)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("M-Pesa query error", e);
            return failResponse("QUERY_ERROR", e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        return failResponse("NOT_SUPPORTED", "M-Pesa refunds must be processed manually via M-Pesa portal");
    }

    private String getAccessToken() {
        try {
            String auth = consumerKey + ":" + consumerSecret;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);

            String url = getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            Map<String, Object> body = response.getBody();
            return body != null ? (String) body.get("access_token") : null;
        } catch (Exception e) {
            log.error("Failed to get M-Pesa access token", e);
            return null;
        }
    }

    private String getBaseUrl() {
        return "production".equalsIgnoreCase(environment)
                ? "https://api.safaricom.co.ke"
                : "https://sandbox.safaricom.co.ke";
    }

    private String getTimestamp() {
        return new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "254700000000";
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "254" + cleaned.substring(1);
        }
        if (cleaned.startsWith("254")) {
            return cleaned;
        }
        return "254" + cleaned;
    }

    private boolean isConfigured() {
        return consumerKey != null && !consumerKey.isBlank()
                && consumerSecret != null && !consumerSecret.isBlank();
    }

    private PaymentGatewayResponse failResponse(String code, String desc) {
        return PaymentGatewayResponse.builder()
                .success(false)
                .status(PaymentGatewayResponse.Status.FAILED.name())
                .responseCode(code)
                .responseDescription(desc)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
