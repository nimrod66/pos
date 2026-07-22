package com.example.pos.integration.fiscal.client;

import com.example.pos.integration.fiscal.config.FiscalProperties;
import com.example.pos.integration.fiscal.dto.v1.FiscalHealthResponse;
import com.example.pos.integration.fiscal.dto.v1.FiscalSaleRequest;
import com.example.pos.integration.fiscal.dto.v1.FiscalSaleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;

public class RestFiscalClient implements FiscalClient {

    private static final Logger log = LoggerFactory.getLogger(RestFiscalClient.class);

    private final RestClient restClient;
    private final FiscalProperties properties;

    public RestFiscalClient(FiscalProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getRemoteUrl())
                .defaultHeader("X-API-Key", properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public FiscalSaleResponse sendInvoice(FiscalSaleRequest request) {
        long start = System.currentTimeMillis();
        try {
            FiscalSaleResponse response = restClient.post()
                    .uri("/api/v1/invoices/issue")
                    .body(request)
                    .retrieve()
                    .body(FiscalSaleResponse.class);

            log.info("Remote fiscal invoice {} created in {}ms",
                    response != null ? response.invoiceNumber() : "null",
                    System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("Remote fiscal sendInvoice failed after {}ms: {}",
                    System.currentTimeMillis() - start, e.getMessage());
            throw new RuntimeException("Fiscal remote call failed", e);
        }
    }

    @Override
    public FiscalHealthResponse health() {
        long start = System.currentTimeMillis();
        try {
            FiscalHealthResponse response = restClient.get()
                    .uri("/api/v1/health")
                    .retrieve()
                    .body(FiscalHealthResponse.class);

            long latency = System.currentTimeMillis() - start;
            return new FiscalHealthResponse("CONNECTED", properties.getMode().name(),
                    properties.getRemoteUrl(), latency);
        } catch (Exception e) {
            log.warn("Remote fiscal health check failed after {}ms: {}",
                    System.currentTimeMillis() - start, e.getMessage());
            return new FiscalHealthResponse("OFFLINE", properties.getMode().name(),
                    properties.getRemoteUrl(), System.currentTimeMillis() - start);
        }
    }
}
