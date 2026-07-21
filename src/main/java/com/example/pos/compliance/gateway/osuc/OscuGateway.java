package com.example.pos.compliance.gateway.osuc;

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

@Component
public class OscuGateway implements ComplianceGateway {

    private static final Logger log = LoggerFactory.getLogger(OscuGateway.class);

    private final HttpClient httpClient;
    private final String apiUrl;
    private final int timeout;
    private final OscuMapper mapper;

    public OscuGateway(OscuMapper mapper,
                       @Value("${compliance.osuc.api-url:}") String apiUrl,
                       @Value("${compliance.osuc.timeout-seconds:30}") int timeout) {
        this.mapper = mapper;
        this.apiUrl = apiUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public String getProviderName() {
        return "OSCU";
    }

    @Override
    public ComplianceResponse submit(TaxInvoice invoice, String payload) {
        long start = System.currentTimeMillis();

        if (apiUrl == null || apiUrl.isBlank()) {
            return sandboxResponse(start);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/invoices"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(timeout))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            return ComplianceResponse.builder()
                    .success(response.statusCode() >= 200 && response.statusCode() < 300)
                    .statusCode(String.valueOf(response.statusCode()))
                    .receiptNumber(mapper.extractReceiptNumber(response.body()))
                    .message("OSCU response")
                    .rawResponse(response.body())
                    .durationMs(duration)
                    .build();
        } catch (Exception e) {
            log.error("OSCU submission failed for invoice {}", invoice.getInvoiceNumber(), e);
            return ComplianceResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @Override
    public ComplianceResponse queryStatus(String invoiceNumber) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return ComplianceResponse.builder()
                    .success(true)
                    .message("Sandbox mode - invoice not tracked")
                    .build();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/invoices/" + invoiceNumber))
                    .GET()
                    .timeout(Duration.ofSeconds(timeout))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ComplianceResponse.builder()
                    .success(response.statusCode() == 200)
                    .rawResponse(response.body())
                    .build();
        } catch (Exception e) {
            return ComplianceResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    @Override
    public String getHealth() {
        if (apiUrl == null || apiUrl.isBlank()) return "OSCU: SANDBOX";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/health")).GET()
                    .timeout(Duration.ofSeconds(5)).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return "OSCU: UNREACHABLE - " + e.getMessage();
        }
    }

    @Override
    public boolean supports(String providerCode) {
        return "OSCU".equalsIgnoreCase(providerCode);
    }

    private ComplianceResponse sandboxResponse(long start) {
        return ComplianceResponse.builder()
                .success(true)
                .statusCode("200")
                .receiptNumber("SBX-" + System.currentTimeMillis())
                .message("OSCU sandbox - not sent to KRA")
                .rawResponse("{\"receiptNumber\":\"SBX-" + System.currentTimeMillis() + "\",\"status\":\"SANDBOX\"}")
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
