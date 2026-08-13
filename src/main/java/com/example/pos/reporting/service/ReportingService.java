package com.example.pos.reporting.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.systemsettings.service.SystemSettingsService;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.reporting.dto.DashboardReportDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final List<Sales.SaleStatus> COMPLETED_SALE_STATUSES =
            List.of(Sales.SaleStatus.COMPLETED, Sales.SaleStatus.DONE);

    private final SalesRepository salesRepository;
    private final SaleReturnsRepository returnsRepository;
    private final StockRepository stockRepository;
    private final MedicineRepository medicineRepository;
    private final BranchRepository branchRepository;
    private final SystemSettingsService settingsService;
    private final AuthenticatedUserContext current;
    private final Clock clock;

    public ReportingService(SalesRepository salesRepository,
                            SaleReturnsRepository returnsRepository,
                            StockRepository stockRepository,
                            MedicineRepository medicineRepository,
                            BranchRepository branchRepository,
                            SystemSettingsService settingsService,
                            AuthenticatedUserContext current,
                            Clock clock) {
        this.salesRepository = salesRepository;
        this.returnsRepository = returnsRepository;
        this.stockRepository = stockRepository;
        this.medicineRepository = medicineRepository;
        this.branchRepository = branchRepository;
        this.settingsService = settingsService;
        this.current = current;
        this.clock = clock;
    }

    public DashboardReportDto getDashboard(UUID branchId, LocalDate date) {
        return getDashboard(branchId, date, false);
    }

    public DashboardReportDto getDashboard(UUID branchId, LocalDate date,
                                           boolean pharmacyWide) {
        List<Branch> branches = resolveBranches(branchId, pharmacyWide);
        LocalDate reportDate = date == null ? LocalDate.now(clock) : date;
        SalesReportDto sales = getSalesReportInternal(
                branchId, branches, reportDate, reportDate, pharmacyWide);
        InventoryReportDto inventory = getInventoryReportInternal(
                branchId, branches, reportDate, pharmacyWide);
        return new DashboardReportDto(
                pharmacyWide ? null : branchId,
                pharmacyWide,
                reportDate,
                sales.completedSalesCount(),
                sales.grossSales(),
                sales.refunds(),
                sales.netSales(),
                inventory.lowStockCount(),
                inventory.batchCount(),
                inventory.nearExpiryCount(),
                inventory.expiredCount(),
                inventory.nearExpiryDays());
    }

    // Kept for the existing GraphQL contract while REST clients use the typed DTO.
    public Map<String, Object> getDashboard(UUID branchId) {
        DashboardReportDto report = getDashboard(branchId, null);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("todaySalesCount", report.completedSalesCount());
        dashboard.put("todaySalesTotal", report.netSales());
        dashboard.put("lowStockCount", report.lowStockCount());
        dashboard.put("totalStockItems", report.totalStockItems());
        return dashboard;
    }

    public SalesReportDto getSalesReport(UUID branchId, LocalDate from, LocalDate to) {
        return getSalesReport(branchId, from, to, false);
    }

    public SalesReportDto getSalesReport(UUID branchId, LocalDate from, LocalDate to,
                                         boolean pharmacyWide) {
        List<Branch> branches = resolveBranches(branchId, pharmacyWide);
        validateDateRange(from, to);
        return getSalesReportInternal(branchId, branches, from, to, pharmacyWide);
    }

    public InventoryReportDto getInventoryReport(UUID branchId, LocalDate asOf) {
        return getInventoryReport(branchId, asOf, false);
    }

    public InventoryReportDto getInventoryReport(UUID branchId, LocalDate asOf,
                                                 boolean pharmacyWide) {
        List<Branch> branches = resolveBranches(branchId, pharmacyWide);
        return getInventoryReportInternal(
                branchId, branches, asOf == null ? LocalDate.now(clock) : asOf, pharmacyWide);
    }

    private SalesReportDto getSalesReportInternal(UUID branchId, List<Branch> branches,
                                                   LocalDate from, LocalDate to,
                                                   boolean pharmacyWide) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<UUID> branchIds = branches.stream().map(Branch::getId).toList();
        List<Sales> sales = salesRepository
                .findByBranchIdInAndSaleStatusInAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        branchIds, COMPLETED_SALE_STATUSES, start, end);
        List<SaleReturns> returns = returnsRepository
                .findByBranchIdInAndStatusIgnoreCaseAndReturnDateGreaterThanEqualAndReturnDateLessThan(
                        branchIds, "COMPLETED", start, end);

        BigDecimal grossSales = sumSales(sales);
        BigDecimal refunds = returns.stream()
                .map(SaleReturns::getRefundAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentTotals payments = paymentTotals(sales);
        PaymentTotals refundPayments = refundTotals(returns);
        List<SalesReportDto.TopProductDto> topProducts = topProducts(sales, returns);

        return new SalesReportDto(
                pharmacyWide ? null : branchId,
                pharmacyWide,
                from,
                to,
                sales.size(),
                money(grossSales),
                money(refunds),
                money(grossSales.subtract(refunds)),
                money(payments.cash()),
                money(payments.mpesa()),
                money(payments.other()),
                money(refundPayments.cash()),
                money(refundPayments.mpesa()),
                money(refundPayments.other()),
                topProducts);
    }

    private InventoryReportDto getInventoryReportInternal(UUID branchId, List<Branch> branches,
                                                           LocalDate asOf,
                                                           boolean pharmacyWide) {
        List<UUID> branchIds = branches.stream().map(Branch::getId).toList();
        List<Stock> stocks = stockRepository.findByBranchIdIn(branchIds);
        List<Medicine> medicines = medicineRepository.findAllByPharmacyId(current.pharmacyId());
        Map<UUID, Integer> expiryDaysByBranch = new HashMap<>();
        for (Branch branch : branches) {
            expiryDaysByBranch.put(branch.getId(), expiryAlertDays(branch.getId()));
        }
        int nearExpiryDays = pharmacyWide ? 0 : expiryDaysByBranch.get(branchId);
        Map<BranchMedicineKey, Integer> sellableByMedicine = new HashMap<>();
        BigDecimal stockValue = BigDecimal.ZERO;
        int batchCount = 0;
        int nearExpiryCount = 0;
        int expiredCount = 0;

        for (Stock stock : stocks) {
            int quantity = stock.getQuantityAvailable() == null ? 0 : stock.getQuantityAvailable();
            if (quantity <= 0 || stock.getMedicineBatches() == null
                    || stock.getMedicineBatches().getMedicine() == null) {
                continue;
            }
            batchCount++;
            BigDecimal buyingPrice = zeroIfNull(stock.getMedicineBatches().getBuyingPrice());
            stockValue = stockValue.add(buyingPrice.multiply(BigDecimal.valueOf(quantity)));

            LocalDate expiryDate = stock.getMedicineBatches().getExpirationDate();
            boolean sellable = expiryDate == null || expiryDate.isAfter(asOf);
            Medicine medicine = stock.getMedicineBatches().getMedicine();
            UUID stockBranchId = stock.getBranch().getId();
            if (sellable && medicine.getStatus() == Medicine.Status.AVAILABLE) {
                sellableByMedicine.merge(
                        new BranchMedicineKey(stockBranchId, medicine.getId()),
                        quantity,
                        Integer::sum);
            }
            if (expiryDate != null && !expiryDate.isAfter(asOf)) {
                expiredCount++;
            } else if (expiryDate != null && !expiryDate.isAfter(
                    asOf.plusDays(expiryDaysByBranch.get(stockBranchId)))) {
                nearExpiryCount++;
            }
        }

        List<InventoryReportDto.LowStockItemDto> lowStockItems = branches.stream()
                .flatMap(branch -> medicines.stream()
                        .filter(medicine -> medicine.getStatus() == Medicine.Status.AVAILABLE)
                        .map(medicine -> {
                            int available = sellableByMedicine.getOrDefault(
                                    new BranchMedicineKey(branch.getId(), medicine.getId()), 0);
                            int reorderLevel = medicine.getReorderLevel() == null
                                    ? 0 : medicine.getReorderLevel();
                            return new InventoryReportDto.LowStockItemDto(
                                    branch.getId(),
                                    branch.getBranchName(),
                                    medicine.getId(),
                                    medicine.getBrandName(),
                                    medicine.getSku(),
                                    available,
                                    reorderLevel);
                        }))
                .filter(item -> item.available() <= item.reorderLevel())
                .sorted(Comparator.comparing(InventoryReportDto.LowStockItemDto::branchName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparingInt(InventoryReportDto.LowStockItemDto::available)
                        .thenComparing(InventoryReportDto.LowStockItemDto::medicineName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new InventoryReportDto(
                pharmacyWide ? null : branchId,
                pharmacyWide,
                asOf,
                money(stockValue),
                lowStockItems.size(),
                batchCount,
                nearExpiryCount,
                expiredCount,
                nearExpiryDays,
                lowStockItems);
    }

    private List<Branch> resolveBranches(UUID branchId, boolean pharmacyWide) {
        if (!pharmacyWide) {
            current.requireBranch(branchId);
            return List.of(current.branch());
        }
        if (!current.hasAuthority("ROLE_OWNER")) {
            throw new ForbiddenException("Pharmacy-wide reports require the owner role");
        }
        return branchRepository.findByPharmacyId(current.pharmacyId()).stream()
                .filter(branch -> branch.getStatus() == Branch.Status.ACTIVE)
                .toList();
    }

    private List<SalesReportDto.TopProductDto> topProducts(List<Sales> sales,
                                                            List<SaleReturns> returns) {
        Map<UUID, ProductTotals> totals = new LinkedHashMap<>();
        for (Sales sale : sales) {
            for (SaleItems item : sale.getSaleItems()) {
                Medicine medicine = item.getMedicineBatches().getMedicine();
                ProductTotals total = totals.computeIfAbsent(medicine.getId(),
                        ignored -> new ProductTotals(medicine.getBrandName()));
                total.quantity += item.getQuantity() == null ? 0 : item.getQuantity();
                total.revenue = total.revenue.add(zeroIfNull(item.getTotal()));
            }
        }
        for (SaleReturns saleReturn : returns) {
            for (SaleReturnItems item : saleReturn.getSaleReturnItems()) {
                Medicine medicine = item.getMedicineBatches().getMedicine();
                ProductTotals total = totals.computeIfAbsent(medicine.getId(),
                        ignored -> new ProductTotals(medicine.getBrandName()));
                total.quantity -= item.getQuantity() == null ? 0 : item.getQuantity();
                total.revenue = total.revenue.subtract(zeroIfNull(item.getRefundAmount()));
            }
        }
        List<SalesReportDto.TopProductDto> products = new ArrayList<>();
        totals.forEach((medicineId, total) -> products.add(new SalesReportDto.TopProductDto(
                medicineId, total.name, total.quantity, money(total.revenue))));
        return products.stream()
                .sorted(Comparator.comparing(SalesReportDto.TopProductDto::netRevenue).reversed())
                .limit(6)
                .toList();
    }

    private PaymentTotals paymentTotals(List<Sales> sales) {
        PaymentTotals totals = new PaymentTotals();
        for (Sales sale : sales) {
            for (Payment payment : sale.getPayment()) {
                if (!"COMPLETED".equalsIgnoreCase(payment.getPaymentStatus())) continue;
                totals.add(payment.getPaymentMethod() == null ? "" : payment.getPaymentMethod().name(),
                        zeroIfNull(payment.getAmount()));
            }
        }
        return totals;
    }

    private PaymentTotals refundTotals(List<SaleReturns> returns) {
        PaymentTotals totals = new PaymentTotals();
        for (SaleReturns saleReturn : returns) {
            totals.add(saleReturn.getRefundMethod(), zeroIfNull(saleReturn.getRefundAmount()));
        }
        return totals;
    }

    private BigDecimal sumSales(List<Sales> sales) {
        return sales.stream()
                .map(Sales::getTotal)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int expiryAlertDays(UUID branchId) {
        String configured = settingsService.resolveSettingValue(
                "inventory.expiry_alert_days", branchId, current.pharmacyId(), "90");
        try {
            return Math.max(1, Math.min(365, Integer.parseInt(configured)));
        } catch (NumberFormatException ex) {
            return 90;
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("A valid report date range is required", "INVALID_DATE_RANGE");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new BadRequestException("Report ranges cannot exceed 366 days", "DATE_RANGE_TOO_LARGE");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return zeroIfNull(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record BranchMedicineKey(UUID branchId, UUID medicineId) {
    }

    private static final class ProductTotals {
        private final String name;
        private int quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductTotals(String name) {
            this.name = name;
        }
    }

    private static final class PaymentTotals {
        private BigDecimal cash = BigDecimal.ZERO;
        private BigDecimal mpesa = BigDecimal.ZERO;
        private BigDecimal other = BigDecimal.ZERO;

        private void add(String method, BigDecimal amount) {
            String normalized = method == null ? "" : method.toUpperCase(Locale.ROOT);
            if (normalized.contains("MPESA") || normalized.contains("M_PESA")) {
                mpesa = mpesa.add(amount);
            } else if (normalized.contains("CASH")) {
                cash = cash.add(amount);
            } else {
                other = other.add(amount);
            }
        }

        private BigDecimal cash() {
            return cash;
        }

        private BigDecimal mpesa() {
            return mpesa;
        }

        private BigDecimal other() {
            return other;
        }
    }
}
