package com.example.pos.reporting.service;

import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.finance.expenses.repository.ExpensesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final SalesRepository salesRepository;
    private final StockRepository stockRepository;
    private final ExpensesRepository expensesRepository;

    public ReportingService(SalesRepository salesRepository, StockRepository stockRepository,
                            ExpensesRepository expensesRepository) {
        this.salesRepository = salesRepository;
        this.stockRepository = stockRepository;
        this.expensesRepository = expensesRepository;
    }

    public Map<String, Object> getDashboard(UUID branchId) {
        Map<String, Object> dashboard = new HashMap<>();

        var todaySales = salesRepository.findByBranchIdAndCreatedAtBetween(
                branchId, LocalDateTime.now().withHour(0).withMinute(0),
                LocalDateTime.now().withHour(23).withMinute(59));

        BigDecimal totalSales = todaySales.stream()
                .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.put("todaySalesCount", todaySales.size());
        dashboard.put("todaySalesTotal", totalSales);
        dashboard.put("lowStockCount", stockRepository
                .findByBranchIdAndQuantityAvailableLessThanEqual(branchId, 10).size());
        dashboard.put("totalStockItems", stockRepository.findByBranchId(branchId).size());

        return dashboard;
    }
}
