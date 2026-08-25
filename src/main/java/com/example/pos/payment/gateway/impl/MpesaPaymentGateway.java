package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import com.example.pos.sale.payment.service.MpesaSettings;
import lombok.extern.slf4j.Slf4j;
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

    private final MpesaSettings mpesaSettings;
    private final RestTemplate restTemplate = new RestTemplate();

    public MpesaPaymentGateway(MpesaSettings mpesaSettings) {
        this.mpesaSettings = mpesaSettings;
    }

    private MpesaSettings.Config config() {
        return mpesaSettings.resolve();
    }

    @Override
    public String getType() {
        return "M_PESA";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        MpesaSettings.Config cfg = config();
        if (!cfg.stkReady()) {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("CONFIG_ERROR")
                    .responseDescription("M-Pesa is not configured for this pharmacy. Set the Daraja credentials in Settings.")
                    .build();
        }

        try {
            String phoneNumber = sanitizePhone(request.getPhoneNumber());
            if (phoneNumber == null) {
                return failResponse("INVALID_PHONE",
                        "Enter a valid Kenyan M-Pesa phone number");
            }
            String accessToken = getAccessToken(cfg);
            if (accessToken == null) {
                return failResponse("AUTH_ERROR", "Failed to get M-Pesa access token");
            }

            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                    (cfg.shortcode() + cfg.passkey() + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", cfg.shortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("TransactionType", "CustomerPayBillOnline");
            body.put("Amount", request.getAmount().intValue());
            body.put("PartyA", phoneNumber);
            body.put("PartyB", cfg.shortcode());
            body.put("PhoneNumber", phoneNumber);
            body.put("CallBackURL", cfg.callbackUrl());
            body.put("AccountReference", request.getAccountReference() != null
                    ? request.getAccountReference() : request.getReference());
            body.put("TransactionDesc", request.getDescription() != null
                    ? request.getDescription() : "Pharmacy Purchase");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = getBaseUrl(cfg) + "/mpesa/stkpush/v1/processrequest";
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
            return failResponse(
                    "PROCESSING_ERROR",
                    "M-Pesa did not return a definitive response. Verify the payment before retrying");
        }
    }

    @Override
    public PaymentGatewayResponse queryStatus(String checkoutRequestId) {
        MpesaSettings.Config cfg = config();
        if (!cfg.stkReady()) {
            return failResponse("CONFIG_ERROR", "M-Pesa is not configured for this pharmacy");
        }
        try {
            String accessToken = getAccessToken(cfg);
            if (accessToken == null) {
                return failResponse("AUTH_ERROR", "Failed to get access token");
            }

            String timestamp = getTimestamp();
            String password = Base64.getEncoder().encodeToString(
                    (cfg.shortcode() + cfg.passkey() + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", cfg.shortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = getBaseUrl(cfg) + "/mpesa/stkpushquery/v1/query";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<String, Object> respBody = response.getBody();
            Object resultCodeValue = respBody != null ? respBody.get("ResultCode") : null;
            String resultCode = resultCodeValue == null ? null : String.valueOf(resultCodeValue);
            String responseCode = respBody != null && respBody.get("ResponseCode") != null
                    ? String.valueOf(respBody.get("ResponseCode")) : null;

            String status;
            if ("0".equals(resultCode)) {
                status = PaymentGatewayResponse.Status.COMPLETED.name();
            } else if ("1032".equals(resultCode)) {
                status = PaymentGatewayResponse.Status.CANCELLED.name();
            } else if (resultCode == null) {
                status = PaymentGatewayResponse.Status.PROCESSING.name();
            } else {
                status = PaymentGatewayResponse.Status.FAILED.name();
            }

            return PaymentGatewayResponse.builder()
                    .success("0".equals(resultCode)
                            || PaymentGatewayResponse.Status.PROCESSING.name().equals(status))
                    .status(status)
                    .responseCode(resultCode != null ? resultCode : responseCode)
                    .responseDescription((String) respBody.get("ResultDesc"))
                    .checkoutRequestId(checkoutRequestId)
                    .rawResponse(respBody != null ? respBody.toString() : null)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("M-Pesa query error", e);
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status(PaymentGatewayResponse.Status.PROCESSING.name())
                    .responseCode("QUERY_ERROR")
                    .responseDescription("M-Pesa status is temporarily unavailable; the payment remains pending")
                    .checkoutRequestId(checkoutRequestId)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        return failResponse("NOT_SUPPORTED", "M-Pesa refunds must be processed manually via M-Pesa portal");
    }

    private String getAccessToken(MpesaSettings.Config cfg) {
        try {
            String auth = cfg.consumerKey() + ":" + cfg.consumerSecret();
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);

            String url = getBaseUrl(cfg) + "/oauth/v1/generate?grant_type=client_credentials";
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

    private String getBaseUrl(MpesaSettings.Config cfg) {
        return "production".equalsIgnoreCase(cfg.environment())
                ? "https://api.safaricom.co.ke"
                : "https://sandbox.safaricom.co.ke";
    }

    private String getTimestamp() {
        return new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
    }

    private String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            cleaned = "254" + cleaned.substring(1);
        } else if (!cleaned.startsWith("254")) {
            cleaned = "254" + cleaned;
        }
        return cleaned.matches("254(7|1)\\d{8}") ? cleaned : null;
    }

    private boolean isConfigured() {
        return config().stkReady();
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
