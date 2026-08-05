package com.example.pos.reporting.graphql;

import com.example.pos.reporting.service.ReportingService;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class ReportingGraphQLController {

    private final ReportingService reportingService;
    private final SalesRepository salesRepository;

    public ReportingGraphQLController(ReportingService reportingService, SalesRepository salesRepository) {
        this.reportingService = reportingService;
        this.salesRepository = salesRepository;
    }

    @QueryMapping
    public Map<String, Object> dashboard(@Argument UUID branchId) {
        return reportingService.getDashboard(branchId);
    }

    @QueryMapping
    public Sales sale(@Argument UUID id) {
        return salesRepository.findById(id).orElse(null);
    }
}
