package com.example.pos.compliance.receipt.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.receipt.dto.ReceiptDTO;
import com.example.pos.compliance.receipt.service.ReceiptAssembler;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.integration.fiscal.snapshot.FiscalSaleSnapshot;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.terminal.auth.TerminalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/receipts/fiscal")
@RequiredArgsConstructor
public class UnifiedReceiptController {

    private final ReceiptAssembler receiptAssembler;
    private final SalesRepository salesRepository;
    private final TaxInvoiceRepository taxInvoiceRepository;
    private final PharmacyRepository pharmacyRepository;

    @GetMapping("/{saleId}")
    public ResponseEntity<ApiResponse<ReceiptDTO>> getReceipt(@PathVariable UUID saleId) {
        var sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        var invoice = taxInvoiceRepository.findBySaleId(saleId).orElse(null);

        var branch = sale.getBranch();
        var pharmacy = findPharmacy(branch);
        var terminal = TerminalContext.getCurrentTerminal().orElse(null);

        String cashierName = sale.getUser() != null
                ? sale.getUser().getFirstName() + " " + (sale.getUser().getLastName() != null ? sale.getUser().getLastName() : "")
                : null;
        String cashierId = sale.getUser() != null ? String.valueOf(sale.getUser().getId()) : null;
        String customerName = sale.getCustomer() != null
                ? sale.getCustomer().getFirstName() + " " + (sale.getCustomer().getLastName() != null ? sale.getCustomer().getLastName() : "")
                : null;

        var items = new ArrayList<FiscalSaleSnapshot.SnapshotItem>();
        if (sale.getSaleItems() != null) {
            int idx = 1;
            for (var item : sale.getSaleItems()) {
                BigDecimal lineDiscount = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
                BigDecimal lineSubtotal = item.getPrice() != null
                        ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())) : BigDecimal.ZERO;
                BigDecimal taxableAmount = lineSubtotal.subtract(lineDiscount);
                BigDecimal taxAmount = item.getTax() != null ? item.getTax() : BigDecimal.ZERO;
                BigDecimal lineTotal = item.getTotal() != null ? item.getTotal()
                        : lineSubtotal.subtract(lineDiscount).add(taxAmount);

                String productName = item.getMedicineBatches() != null
                        && item.getMedicineBatches().getMedicine() != null
                        ? item.getMedicineBatches().getMedicine().getBrandName() : "Unknown";
                String barcode = item.getMedicineBatches() != null
                        && item.getMedicineBatches().getMedicine() != null
                        ? item.getMedicineBatches().getMedicine().getBarcode() : null;

                items.add(new FiscalSaleSnapshot.SnapshotItem(
                        idx++, productName, barcode, item.getQuantity(), item.getPrice(),
                        lineDiscount, taxableAmount, item.getTaxRate(), null, taxAmount, lineTotal));
            }
        }

        var payments = new ArrayList<FiscalSaleSnapshot.SnapshotPayment>();
        if (sale.getPayment() != null) {
            for (var p : sale.getPayment()) {
                payments.add(new FiscalSaleSnapshot.SnapshotPayment(
                        p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "CASH",
                        p.getAmount(), p.getTransactionReference()));
            }
        }

        BigDecimal discountTotal = sale.getSaleItems() != null
                ? sale.getSaleItems().stream()
                    .map(i -> i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        var snapshot = new FiscalSaleSnapshot(
                saleId,
                invoice != null ? invoice.getInvoiceNumber() : sale.getInvoiceNumber(),
                invoice != null ? invoice.getInvoiceNumber() : null,
                branch != null ? branch.getBranchName() : null,
                cashierName, cashierId,
                pharmacy != null ? pharmacy.getName() : null,
                pharmacy != null ? pharmacy.getAddress() : null,
                pharmacy != null ? pharmacy.getPhoneNumber() : null,
                pharmacy != null ? pharmacy.getKraPin() : null,
                invoice != null ? invoice.getQrCodeContent() : null,
                invoice != null ? invoice.getVerificationUrl() : null,
                sale.getSubtotal(), discountTotal, sale.getTax(), sale.getTotal(),
                "KES", sale.getCreatedAt(), customerName, null,
                terminal != null ? terminal.getTerminalId() : null,
                terminal != null ? terminal.getName() : null,
                terminal != null && terminal.getTerminalType() != null ? terminal.getTerminalType().name() : null,
                items, payments
        );

        ReceiptDTO dto = receiptAssembler.assemble(snapshot);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    private com.example.pos.core.pharmacy.model.Pharmacy findPharmacy(
            com.example.pos.core.branch.model.Branch branch) {
        if (branch == null) return null;
        if (branch.getPharmacy() != null) return branch.getPharmacy();
        return pharmacyRepository.findAll().stream().findFirst().orElse(null);
    }
}
