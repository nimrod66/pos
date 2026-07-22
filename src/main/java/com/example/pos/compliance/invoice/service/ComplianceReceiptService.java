package com.example.pos.compliance.invoice.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.model.Receipt;
import com.example.pos.compliance.invoice.repository.ReceiptRepository;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ComplianceReceiptService {

    private final ReceiptRepository receiptRepo;
    private final DocumentNumberGenerator numberGenerator;

    public ComplianceReceiptService(ReceiptRepository receiptRepo, DocumentNumberGenerator numberGenerator) {
        this.receiptRepo = receiptRepo;
        this.numberGenerator = numberGenerator;
    }

    public Receipt create(Long saleId, Long invoiceId, String receiptDataJson,
                           String businessName, String kraPin, String branchCode,
                           String qrCodeContent, String verificationUrl) {
        Receipt receipt = Receipt.builder()
                .saleId(saleId)
                .invoiceId(invoiceId)
                .receiptNumber(numberGenerator.generate("RCT", branchCode))
                .receiptData(receiptDataJson)
                .printedDate(LocalDateTime.now())
                .reprintCount(0)
                .businessName(businessName)
                .kraPin(kraPin)
                .qrCodeContent(qrCodeContent)
                .verificationUrl(verificationUrl)
                .build();

        return receiptRepo.save(receipt);
    }

    @Transactional(readOnly = true)
    public Receipt getById(Long id) {
        return receiptRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", id));
    }

    @Transactional(readOnly = true)
    public Receipt getBySaleId(Long saleId) {
        return receiptRepo.findBySaleId(saleId).orElse(null);
    }

    public Receipt reprint(Long id) {
        Receipt receipt = getById(id);
        receipt.setReprintCount(receipt.getReprintCount() + 1);
        receipt.setPrintedDate(LocalDateTime.now());
        return receiptRepo.save(receipt);
    }
}
