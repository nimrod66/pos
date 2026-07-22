package com.example.pos.compliance.gateway.vscu;

import com.example.pos.compliance.transmission.batch.model.Batch;
import com.example.pos.compliance.gateway.ComplianceGateway;
import com.example.pos.compliance.gateway.ComplianceResponse;
import com.example.pos.compliance.invoice.model.TaxInvoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class VscuGateway implements ComplianceGateway {

    private static final Logger log = LoggerFactory.getLogger(VscuGateway.class);

    private final HttpClient httpClient;
    private final String apiUrl;
    private final int timeout;

    public VscuGateway(@Value("${compliance.vscu.api-url:}") String apiUrl,
                       @Value("${compliance.vscu.timeout-seconds:30}") int timeout) {
        this.apiUrl = apiUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public String getProviderName() {
        return "VSCU";
    }

    @Override
    public ComplianceResponse submit(TaxInvoice invoice, String payload) {
        long start = System.currentTimeMillis();

        if (apiUrl == null || apiUrl.isBlank()) {
            return ComplianceResponse.builder()
                    .success(true)
                    .receiptNumber("VSCU-SBX-" + System.currentTimeMillis())
                    .message("VSCU sandbox")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/submit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(timeout))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ComplianceResponse.builder()
                    .success(response.statusCode() >= 200 && response.statusCode() < 300)
                    .statusCode(String.valueOf(response.statusCode()))
                    .rawResponse(response.body())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            return ComplianceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    public ComplianceResponse submitBatch(Batch batch, List<String> payloads) {
        long start = System.currentTimeMillis();

        if (apiUrl == null || apiUrl.isBlank()) {
            return ComplianceResponse.builder()
                    .success(true)
                    .message("VSCU batch sandbox - " + payloads.size() + " invoices")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            String json = "{\"batchId\":\"" + batch.getId() + "\",\"invoices\":" + payloads + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/batch"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(timeout * 2))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ComplianceResponse.builder()
                    .success(response.statusCode() >= 200 && response.statusCode() < 300)
                    .rawResponse(response.body())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            return ComplianceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public ComplianceResponse queryStatus(String invoiceNumber) {
        return ComplianceResponse.builder()
                .success(true).message("VSCU status query not supported individually")
                .build();
    }

    @Override
    public String getHealth() {
        if (apiUrl == null || apiUrl.isBlank()) return "VSCU: SANDBOX";
        return "VSCU: OK";
    }

    @Override
    public boolean supports(String providerCode) {
        return "VSCU".equalsIgnoreCase(providerCode);
    }
}
