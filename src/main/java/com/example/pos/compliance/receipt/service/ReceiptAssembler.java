package com.example.pos.compliance.receipt.service;

import com.example.pos.compliance.receipt.dto.ReceiptDTO;
import com.example.pos.compliance.receipt.dto.ReceiptDTO.PaymentLine;
import com.example.pos.compliance.receipt.dto.ReceiptDTO.ReceiptLine;
import com.example.pos.integration.fiscal.snapshot.FiscalSaleSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReceiptAssembler {

    public ReceiptDTO assemble(FiscalSaleSnapshot snapshot) {
        var lines = snapshot.items().stream()
                .map(this::toLine)
                .collect(Collectors.toList());

        var payments = snapshot.payments().stream()
                .map(this::toPayment)
                .collect(Collectors.toList());

        return ReceiptDTO.builder()
                .receiptNumber(snapshot.receiptNumber())
                .invoiceNumber(snapshot.invoiceNumber())
                .issueDate(snapshot.saleDate())
                .currency(snapshot.currency() != null ? snapshot.currency() : "KES")
                .businessName(snapshot.businessName())
                .branchName(snapshot.branchName())
                .address(snapshot.businessAddress())
                .phone(snapshot.businessPhone())
                .logoBase64(null)
                .kraPin(snapshot.kraPin())
                .qrCodeContent(snapshot.qrCodeContent())
                .verificationUrl(snapshot.verificationUrl())
                .customerName(snapshot.customerName())
                .customerPin(snapshot.customerPin())
                .cashierName(snapshot.cashierName())
                .cashierId(snapshot.cashierId())
                .terminalId(snapshot.terminalId())
                .terminalName(snapshot.terminalName())
                .terminalType(snapshot.terminalType())
                .items(lines)
                .payments(payments)
                .subtotal(snapshot.subtotal())
                .discountTotal(snapshot.discountTotal())
                .taxTotal(snapshot.tax())
                .grandTotal(snapshot.total())
                .footerText("Thank you for your purchase")
                .returnPolicy("Returns accepted within 7 days with receipt")
                .thankYouMessage("Karibu Tena!")
                .build();
    }

    private ReceiptLine toLine(FiscalSaleSnapshot.SnapshotItem item) {
        return ReceiptLine.builder()
                .lineNumber(item.lineNumber())
                .name(item.medicineName())
                .barcode(item.barcode())
                .quantity(item.quantity())
                .unitPrice(item.unitPrice())
                .discount(item.discount() != null ? item.discount() : java.math.BigDecimal.ZERO)
                .taxableAmount(item.taxableAmount())
                .taxRate(item.taxRate())
                .taxCode(item.taxCode())
                .taxAmount(item.taxAmount())
                .total(item.total())
                .build();
    }

    private PaymentLine toPayment(FiscalSaleSnapshot.SnapshotPayment payment) {
        return PaymentLine.builder()
                .method(payment.method())
                .amount(payment.amount())
                .reference(payment.reference())
                .changeGiven(null)
                .build();
    }
}
