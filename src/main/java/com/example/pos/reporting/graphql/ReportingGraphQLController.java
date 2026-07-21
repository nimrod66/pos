package com.example.pos.reporting.graphql;

import com.example.pos.reporting.service.ReportingService;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class ReportingGraphQLController {

    private final ReportingService reportingService;
    private final SalesRepository salesRepository;

    public ReportingGraphQLController(ReportingService reportingService, SalesRepository salesRepository) {
        this.reportingService = reportingService;
        this.salesRepository = salesRepository;
    }

    @QueryMapping
    public Map<String, Object> dashboard(@Argument Long branchId) {
        return reportingService.getDashboard(branchId);
    }

    @QueryMapping
    public Sales sale(@Argument Long id) {
        return salesRepository.findById(id).orElse(null);
    }
}
