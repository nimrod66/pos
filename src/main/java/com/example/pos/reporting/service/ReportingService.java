package com.example.pos.reporting.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.systemsettings.SettingKeys;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.finance.expenses.repository.ExpensesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.reporting.dto.DashboardResponseDto;
import com.example.pos.reporting.dto.InventoryReportResponseDto;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.repository.SaleReturnsRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final SalesRepository salesRepository;
    private final SaleReturnsRepository returnsRepository;
    private final StockRepository stockRepository;
    private final ExpensesRepository expensesRepository;
    private final SystemSettingsRepository settingsRepository;

    @Value("${inventory.low-stock-default:10}")
    private int configuredLowStockDefault;

    public ReportingService(SalesRepository salesRepository,
                            SaleReturnsRepository returnsRepository,
                            StockRepository stockRepository,
                            ExpensesRepository expensesRepository,
                            SystemSettingsRepository settingsRepository) {
        this.salesRepository = salesRepository;
        this.returnsRepository = returnsRepository;
        this.stockRepository = stockRepository;
        this.expensesRepository = expensesRepository;
        this.settingsRepository = settingsRepository;
    }

    public DashboardResponseDto getDashboard(List<Branch> branches, LocalDate from, LocalDate to) {
        if (branches == null || branches.isEmpty()) {
            throw new BadRequestException("No accessible branches found");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        BigDecimal salesTotal = BigDecimal.ZERO;
        BigDecimal refundTotal = BigDecimal.ZERO;
        BigDecimal expensesTotal = BigDecimal.ZERO;
        long salesCount = 0;
        long refundCount = 0;
        long lowStockCount = 0;
        long totalStockItems = 0;

        for (Branch branch : branches) {
            List<Sales> sales = salesRepository
                    .findByBranchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndSaleStatusAndPaymentStatus(
                            branch.getId(), start, endExclusive,
                            Sales.SaleStatus.DONE, Sales.PaymentStatus.PAID);
            salesCount += sales.size();
            salesTotal = salesTotal.add(sumSales(sales));

            List<SaleReturns> returns = returnsRepository
                    .findBySalesBranchIdAndReturnDateGreaterThanEqualAndReturnDateLessThanAndStatus(
                            branch.getId(), start, endExclusive, "COMPLETED");
            refundCount += returns.size();
            refundTotal = refundTotal.add(sumRefunds(returns));

            List<Expenses> expenses = expensesRepository
                    .findByCashDrawersStaffShiftsBranchIdAndExpenseDateGreaterThanEqualAndExpenseDateLessThan(
                            branch.getId(), start, endExclusive);
            expensesTotal = expensesTotal.add(expenses.stream()
                    .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            List<Stock> stock = stockRepository.findByBranchId(branch.getId());
            totalStockItems += stock.size();
            lowStockCount += stock.stream().filter(s -> isLowStock(s, branch)).count();
        }

        Branch first = branches.get(0);
        UUID pharmacyId = first.getPharmacy() != null ? first.getPharmacy().getId() : null;
        return DashboardResponseDto.builder()
                .pharmacyId(pharmacyId)
                .branchId(branches.size() == 1 ? first.getId() : null)
                .branchName(branches.size() == 1 ? first.getBranchName() : "All branches")
                .from(from)
                .to(to)
                .salesCount(salesCount)
                .salesTotal(salesTotal)
                .refundCount(refundCount)
                .refundTotal(refundTotal)
                .netSales(salesTotal.subtract(refundTotal))
                .expensesTotal(expensesTotal)
                .lowStockCount(lowStockCount)
                .totalStockItems(totalStockItems)
                .build();
    }

    public InventoryReportResponseDto getInventoryReport(List<Branch> branches) {
        if (branches == null || branches.isEmpty()) {
            throw new BadRequestException("No accessible branches found");
        }
        long total = 0;
        long low = 0;
        long nearExpiry = 0;
        LocalDate today = LocalDate.now();
        LocalDate expiryLimit = today.plusDays(90);
        for (Branch branch : branches) {
            List<Stock> stock = stockRepository.findByBranchId(branch.getId());
            total += stock.size();
            low += stock.stream().filter(s -> isLowStock(s, branch)).count();
            nearExpiry += stock.stream()
                    .filter(s -> s.getMedicineBatches() != null
                            && s.getMedicineBatches().getExpirationDate() != null
                            && !s.getMedicineBatches().getExpirationDate().isBefore(today)
                            && !s.getMedicineBatches().getExpirationDate().isAfter(expiryLimit))
                    .count();
        }
        Branch first = branches.get(0);
        return InventoryReportResponseDto.builder()
                .pharmacyId(first.getPharmacy() != null ? first.getPharmacy().getId() : null)
                .branchId(branches.size() == 1 ? first.getId() : null)
                .branchName(branches.size() == 1 ? first.getBranchName() : "All branches")
                .totalStockItems(total)
                .lowStockItems(low)
                .nearExpiryItems(nearExpiry)
                .build();
    }

    private boolean isLowStock(Stock stock, Branch branch) {
        int threshold = configuredLowStockDefault;
        UUID pharmacyId = branch.getPharmacy() != null ? branch.getPharmacy().getId() : null;
        if (pharmacyId != null) {
            SystemSettings setting = settingsRepository.findSetting(
                    SettingKeys.Inventory.LOW_STOCK_THRESHOLD, branch.getId(), pharmacyId).orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                try {
                    threshold = Integer.parseInt(setting.getSettingValue());
                } catch (NumberFormatException ignored) {
                    // Keep the deployment default when the setting is malformed.
                }
            }
        }
        if (stock.getReorderLevel() != null) {
            threshold = stock.getReorderLevel();
        } else if (stock.getMinimumStock() != null) {
            threshold = stock.getMinimumStock();
        }
        return stock.getQuantityAvailable() != null && stock.getQuantityAvailable() <= threshold;
    }

    private BigDecimal sumSales(List<Sales> sales) {
        return sales.stream().map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRefunds(List<SaleReturns> returns) {
        return returns.stream().flatMap(r -> r.getSaleReturnItems().stream())
                .map(item -> {
                    if (item.getSaleItems() == null || item.getQuantity() == null) return BigDecimal.ZERO;
                    BigDecimal lineTotal = item.getSaleItems().getTotal();
                    Integer soldQuantity = item.getSaleItems().getQuantity();
                    if (lineTotal != null && soldQuantity != null && soldQuantity > 0) {
                        return lineTotal.divide(BigDecimal.valueOf(soldQuantity), 8, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    return item.getSaleItems().getPrice() != null
                            ? item.getSaleItems().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                            : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
