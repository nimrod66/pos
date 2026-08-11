package com.example.pos.reporting.service;

import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.finance.expenses.repository.ExpensesRepository;
import org.springframework.stereotype.Service;
import com.example.pos.security.auth.AuthenticatedUserContext;
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
    private final AuthenticatedUserContext current;

    public ReportingService(SalesRepository salesRepository, StockRepository stockRepository,
                            ExpensesRepository expensesRepository,
                            AuthenticatedUserContext current) {
        this.salesRepository = salesRepository;
        this.stockRepository = stockRepository;
        this.expensesRepository = expensesRepository;
        this.current = current;
    }

    public Map<String, Object> getDashboard(UUID branchId) {
        current.requireBranch(branchId);
        Map<String, Object> dashboard = new HashMap<>();

        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        var todaySales = salesRepository.findByBranchIdAndCreatedAtBetween(
                branchId, start, end);

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
