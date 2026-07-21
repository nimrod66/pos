package com.example.pos.compliance.invoice.service;

import com.example.pos.compliance.invoice.event.InvoiceIssuedEvent;
import com.example.pos.compliance.transmission.service.TransmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InvoiceEventListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventListener.class);

    private final TransmissionService transmissionService;

    public InvoiceEventListener(TransmissionService transmissionService) {
        this.transmissionService = transmissionService;
    }

    @EventListener
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Invoice issued: {} — auto-queuing for eTIMS transmission", event.getInvoiceNumber());
        try {
            transmissionService.createAndQueue(event.getInvoiceId(), "TAX_INVOICE", null);
        } catch (Exception e) {
            log.error("Failed to auto-queue transmission for invoice {}", event.getInvoiceId(), e);
        }
    }
}
