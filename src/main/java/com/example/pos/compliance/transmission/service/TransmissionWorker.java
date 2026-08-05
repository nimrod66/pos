package com.example.pos.compliance.transmission.service;

import com.example.pos.compliance.gateway.ComplianceGateway;
import com.example.pos.compliance.gateway.ComplianceGatewayFactory;
import com.example.pos.compliance.gateway.ComplianceResponse;
import com.example.pos.compliance.gateway.osuc.OscuMapper;
import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.service.InvoiceService;
import com.example.pos.compliance.transmission.model.Transmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransmissionWorker {

    private static final Logger log = LoggerFactory.getLogger(TransmissionWorker.class);

    private final InMemoryTransmissionQueue queue;
    private final TransmissionService transmissionService;
    private final InvoiceService invoiceService;
    private final OscuMapper oscuMapper;
    private final ComplianceGatewayFactory gatewayFactory;

    private volatile boolean running = true;

    public TransmissionWorker(InMemoryTransmissionQueue queue,
                              TransmissionService transmissionService,
                              InvoiceService invoiceService,
                              OscuMapper oscuMapper,
                              ComplianceGatewayFactory gatewayFactory) {
        this.queue = queue;
        this.transmissionService = transmissionService;
        this.invoiceService = invoiceService;
        this.oscuMapper = oscuMapper;
        this.gatewayFactory = gatewayFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        Thread worker = new Thread(this::processQueue, "transmission-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Transmission worker started");
    }

    private void processQueue() {
        while (running) {
            try {
                UUID txId = queue.dequeue();
                if (txId != null) {
                    process(txId);
                } else {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing transmission", e);
            }
        }
    }

    public void process(UUID transmissionId) {
        Transmission tx = transmissionService.getById(transmissionId);
        log.info("Processing transmission {} for invoice {}", tx.getId(), tx.getInvoiceId());

        TaxInvoice invoice = invoiceService.getById(tx.getInvoiceId());
        var payload = oscuMapper.toPayload(invoice);

        String jsonPayload = payload.toString();
        tx = transmissionService.markTransmitting(tx.getId(), jsonPayload);

        long start = System.currentTimeMillis();
        try {
            ComplianceGateway gateway = gatewayFactory.getForInvoice(invoice);
            ComplianceResponse response = gateway.submit(invoice, jsonPayload);
            long duration = System.currentTimeMillis() - start;

            if (response.isSuccess()) {
                transmissionService.markTransmitted(tx.getId(), response.getRawResponse(),
                        response.getReceiptNumber(), duration);
                log.info("Transmission {} completed via {}", tx.getId(), gateway.getProviderName());
            } else {
                transmissionService.markFailed(tx.getId(), response.getMessage(),
                        response.getRawResponse(), Integer.parseInt(response.getStatusCode() != null ? response.getStatusCode() : "0"));
                log.error("Transmission {} failed: {}", tx.getId(), response.getMessage());
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            transmissionService.markFailed(tx.getId(), e.getMessage(), null, 0);
            log.error("Transmission {} failed: {}", tx.getId(), e.getMessage());
        }
    }
}
