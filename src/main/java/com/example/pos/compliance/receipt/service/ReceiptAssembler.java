package com.example.pos.compliance.receipt.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.receipt.dto.ReceiptDTO;
import com.example.pos.compliance.receipt.dto.ReceiptDTO.PaymentLine;
import com.example.pos.compliance.receipt.dto.ReceiptDTO.ReceiptLine;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.terminal.auth.TerminalContext;
import com.example.pos.terminal.auth.TerminalPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReceiptAssembler {

    private final SalesRepository salesRepository;
    private final TaxInvoiceRepository taxInvoiceRepository;
    private final PharmacyRepository pharmacyRepository;

    public ReceiptDTO assemble(Long saleId) {
        Sales sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        Branch branch = sale.getBranch();
        Pharmacy pharmacy = findPharmacy(branch);
        TaxInvoice invoice = taxInvoiceRepository.findBySaleId(saleId).orElse(null);
        TerminalPrincipal terminal = TerminalContext.getCurrentTerminal().orElse(null);

        String cashierName = sale.getUser() != null
                ? sale.getUser().getFirstName() + " " + (sale.getUser().getLastName() != null ? sale.getUser().getLastName() : "")
                : null;
        String cashierId = sale.getUser() != null ? String.valueOf(sale.getUser().getId()) : null;

        List<ReceiptLine> lines = sale.getSaleItems() != null
                ? sale.getSaleItems().stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getId() != null ? a.getId().intValue() : 0,
                            b.getId() != null ? b.getId().intValue() : 0))
                    .map(this::toLine)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        List<PaymentLine> payments = sale.getPayment() != null
                ? sale.getPayment().stream().map(this::toPayment).collect(Collectors.toList())
                : new ArrayList<>();

        return ReceiptDTO.builder()
                .receiptNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : sale.getInvoiceNumber())
                .issueDate(invoice != null ? invoice.getCreatedAt() : sale.getCreatedAt())
                .currency("KES")
                .businessName(pharmacy != null ? pharmacy.getName() : null)
                .branchName(branch != null ? branch.getBranchName() : null)
                .address(pharmacy != null ? pharmacy.getAddress() : null)
                .phone(pharmacy != null ? pharmacy.getPhoneNumber() : null)
                .logoBase64(null)
                .kraPin(pharmacy != null ? pharmacy.getKraPin() : null)
                .qrCodeContent(invoice != null ? invoice.getQrCodeContent() : null)
                .verificationUrl(invoice != null ? invoice.getVerificationUrl() : null)
                .customerName(sale.getCustomer() != null
                        ? sale.getCustomer().getFirstName() + " " + (sale.getCustomer().getLastName() != null ? sale.getCustomer().getLastName() : "")
                        : null)
                .customerPin(null)
                .cashierName(cashierName)
                .cashierId(cashierId)
                .terminalId(terminal != null ? terminal.getTerminalId() : null)
                .terminalName(terminal != null ? terminal.getName() : null)
                .terminalType(terminal != null ? terminal.getTerminalType() != null ? terminal.getTerminalType().name() : null : null)
                .items(lines)
                .payments(payments)
                .subtotal(sale.getSubtotal())
                .discountTotal(calculateDiscountTotal(sale))
                .taxTotal(sale.getTax())
                .grandTotal(sale.getTotal())
                .footerText("Thank you for your purchase")
                .returnPolicy("Returns accepted within 7 days with receipt")
                .thankYouMessage("Karibu Tena!")
                .build();
    }

    private Pharmacy findPharmacy(Branch branch) {
        if (branch == null || branch.getPharmacy() != null) {
            return branch != null ? branch.getPharmacy() : null;
        }
        return pharmacyRepository.findAll().stream().findFirst().orElse(null);
    }

    private ReceiptLine toLine(SaleItems item) {
        BigDecimal lineDiscount = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
        BigDecimal lineSubtotal = item.getPrice() != null
                ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                : BigDecimal.ZERO;
        BigDecimal taxableAmount = lineSubtotal.subtract(lineDiscount);
        BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = item.getTax() != null ? item.getTax() : BigDecimal.ZERO;
        BigDecimal lineTotal = item.getTotal() != null ? item.getTotal() : lineSubtotal.subtract(lineDiscount).add(taxAmount);

        String productName = item.getMedicineBatches() != null
                && item.getMedicineBatches().getMedicine() != null
                ? item.getMedicineBatches().getMedicine().getBrandName() : "Unknown";
        String barcode = item.getMedicineBatches() != null
                && item.getMedicineBatches().getMedicine() != null
                ? item.getMedicineBatches().getMedicine().getBarcode() : null;

        return ReceiptLine.builder()
                .lineNumber(item.getId() != null ? item.getId().intValue() : 0)
                .name(productName)
                .barcode(barcode)
                .quantity(item.getQuantity())
                .unitPrice(item.getPrice())
                .discount(lineDiscount)
                .taxableAmount(taxableAmount)
                .taxRate(taxRate)
                .taxCode(null)
                .taxAmount(taxAmount)
                .total(lineTotal)
                .build();
    }

    private PaymentLine toPayment(Payment payment) {
        return PaymentLine.builder()
                .method(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "CASH")
                .amount(payment.getAmount())
                .reference(payment.getTransactionReference())
                .changeGiven(null)
                .build();
    }

    private BigDecimal calculateDiscountTotal(Sales sale) {
        if (sale.getSaleItems() == null) return BigDecimal.ZERO;
        return sale.getSaleItems().stream()
                .map(i -> i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
