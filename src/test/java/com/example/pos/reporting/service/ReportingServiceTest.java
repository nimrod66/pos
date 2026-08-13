package com.example.pos.reporting.service;

import com.example.pos.core.systemsettings.service.SystemSettingsService;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.reporting.dto.InventoryReportDto;
import com.example.pos.reporting.dto.SalesReportDto;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.repository.SaleReturnsRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    private static final UUID BRANCH_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PHARMACY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MEDICINE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Mock
    private SalesRepository salesRepository;
    @Mock
    private SaleReturnsRepository returnsRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private SystemSettingsService settingsService;
    @Mock
    private AuthenticatedUserContext current;
    @Mock
    private Branch branch;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T09:00:00Z"), ZoneId.of("Africa/Nairobi"));
        service = new ReportingService(
                salesRepository,
                returnsRepository,
                stockRepository,
                medicineRepository,
                branchRepository,
                settingsService,
                current,
                clock);
        when(current.branch()).thenReturn(branch);
        when(branch.getId()).thenReturn(BRANCH_ID);
    }

    @Test
    void salesReportUsesCompletedTimePaymentRowsAndCompletedReturns() {
        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(MEDICINE_ID);
        when(medicine.getBrandName()).thenReturn("Paracetamol 500mg");
        MedicineBatches batch = mock(MedicineBatches.class);
        when(batch.getMedicine()).thenReturn(medicine);

        SaleItems saleItem = SaleItems.builder()
                .medicineBatches(batch)
                .quantity(2)
                .total(new BigDecimal("100.00"))
                .build();
        Payment cash = Payment.builder()
                .paymentMethod(Payment.PaymentMethod.CASH)
                .paymentStatus("COMPLETED")
                .amount(new BigDecimal("60.00"))
                .build();
        Payment mpesa = Payment.builder()
                .paymentMethod(Payment.PaymentMethod.MPESA_MANUAL)
                .paymentStatus("COMPLETED")
                .amount(new BigDecimal("40.00"))
                .build();
        Sales sale = Sales.builder()
                .saleStatus(Sales.SaleStatus.COMPLETED)
                .total(new BigDecimal("100.00"))
                .saleItems(List.of(saleItem))
                .payment(Set.of(cash, mpesa))
                .build();

        SaleReturnItems returnItem = SaleReturnItems.builder()
                .medicineBatches(batch)
                .quantity(1)
                .refundAmount(new BigDecimal("25.00"))
                .build();
        SaleReturns saleReturn = SaleReturns.builder()
                .status("COMPLETED")
                .refundMethod("MPESA_MANUAL")
                .refundAmount(new BigDecimal("25.00"))
                .saleReturnItems(Set.of(returnItem))
                .build();

        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 13);
        when(salesRepository
                .findByBranchIdInAndSaleStatusInAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        List.of(BRANCH_ID),
                        List.of(Sales.SaleStatus.COMPLETED, Sales.SaleStatus.DONE),
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(sale));
        when(returnsRepository
                .findByBranchIdInAndStatusIgnoreCaseAndReturnDateGreaterThanEqualAndReturnDateLessThan(
                        List.of(BRANCH_ID), "COMPLETED", from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(saleReturn));

        SalesReportDto report = service.getSalesReport(BRANCH_ID, from, to);

        assertThat(report.completedSalesCount()).isEqualTo(1);
        assertThat(report.grossSales()).isEqualByComparingTo("100.00");
        assertThat(report.refunds()).isEqualByComparingTo("25.00");
        assertThat(report.netSales()).isEqualByComparingTo("75.00");
        assertThat(report.cashPayments()).isEqualByComparingTo("60.00");
        assertThat(report.mpesaPayments()).isEqualByComparingTo("40.00");
        assertThat(report.mpesaRefunds()).isEqualByComparingTo("25.00");
        assertThat(report.topProducts()).singleElement().satisfies(product -> {
            assertThat(product.medicineId()).isEqualTo(MEDICINE_ID);
            assertThat(product.quantity()).isEqualTo(1);
            assertThat(product.netRevenue()).isEqualByComparingTo("75.00");
        });
        verify(current).requireBranch(BRANCH_ID);
    }

    @Test
    void inventoryReportUsesBatchCostsReorderLevelsAndConfiguredExpiryWindow() {
        LocalDate asOf = LocalDate.of(2026, 8, 13);
        when(branch.getBranchName()).thenReturn("Main Branch");
        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(MEDICINE_ID);
        when(medicine.getBrandName()).thenReturn("Paracetamol 500mg");
        when(medicine.getSku()).thenReturn("PARA-500");
        when(medicine.getStatus()).thenReturn(Medicine.Status.AVAILABLE);
        when(medicine.getReorderLevel()).thenReturn(10);

        MedicineBatches nearExpiryBatch = mock(MedicineBatches.class);
        when(nearExpiryBatch.getMedicine()).thenReturn(medicine);
        when(nearExpiryBatch.getBuyingPrice()).thenReturn(new BigDecimal("12.00"));
        when(nearExpiryBatch.getExpirationDate()).thenReturn(LocalDate.of(2026, 9, 1));
        Stock nearExpiryStock = mock(Stock.class);
        when(nearExpiryStock.getQuantityAvailable()).thenReturn(5);
        when(nearExpiryStock.getMedicineBatches()).thenReturn(nearExpiryBatch);
        when(nearExpiryStock.getBranch()).thenReturn(branch);

        MedicineBatches expiredBatch = mock(MedicineBatches.class);
        when(expiredBatch.getMedicine()).thenReturn(medicine);
        when(expiredBatch.getBuyingPrice()).thenReturn(new BigDecimal("7.00"));
        when(expiredBatch.getExpirationDate()).thenReturn(LocalDate.of(2026, 8, 1));
        Stock expiredStock = mock(Stock.class);
        when(expiredStock.getQuantityAvailable()).thenReturn(2);
        when(expiredStock.getMedicineBatches()).thenReturn(expiredBatch);
        when(expiredStock.getBranch()).thenReturn(branch);

        when(current.pharmacyId()).thenReturn(PHARMACY_ID);
        when(stockRepository.findByBranchIdIn(List.of(BRANCH_ID)))
                .thenReturn(List.of(nearExpiryStock, expiredStock));
        when(medicineRepository.findAllByPharmacyId(PHARMACY_ID))
                .thenReturn(List.of(medicine));
        when(settingsService.resolveSettingValue(
                "inventory.expiry_alert_days", BRANCH_ID, PHARMACY_ID, "90"))
                .thenReturn("30");

        InventoryReportDto report = service.getInventoryReport(BRANCH_ID, asOf);

        assertThat(report.stockValue()).isEqualByComparingTo("74.00");
        assertThat(report.batchCount()).isEqualTo(2);
        assertThat(report.nearExpiryCount()).isEqualTo(1);
        assertThat(report.expiredCount()).isEqualTo(1);
        assertThat(report.nearExpiryDays()).isEqualTo(30);
        assertThat(report.lowStockItems()).singleElement().satisfies(item -> {
            assertThat(item.available()).isEqualTo(5);
            assertThat(item.reorderLevel()).isEqualTo(10);
        });
        verify(current).requireBranch(BRANCH_ID);
    }
}
