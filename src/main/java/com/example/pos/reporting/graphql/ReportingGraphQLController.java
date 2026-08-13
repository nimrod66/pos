package com.example.pos.reporting.graphql;

import com.example.pos.reporting.dto.DashboardResponseDto;
import com.example.pos.reporting.service.ReportingService;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.pos.security.auth.AuthenticatedUserContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
public class ReportingGraphQLController {

    private final ReportingService reportingService;
    private final SalesRepository salesRepository;
    private final AuthenticatedUserContext current;

    public ReportingGraphQLController(ReportingService reportingService,
                                      SalesRepository salesRepository,
                                      AuthenticatedUserContext current) {
        this.reportingService = reportingService;
        this.salesRepository = salesRepository;
        this.current = current;
    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('report.sales.read', 'report.inventory.read')")
    public DashboardResponseDto dashboard(@Argument UUID branchId) {
        current.requireBranch(branchId);
        return reportingService.getDashboard(List.of(current.branch()), LocalDate.now(), LocalDate.now());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('report.sales.read')")
    public Sales sale(@Argument UUID id) {
        return salesRepository.findDetailedByIdAndBranchId(id, current.branch().getId()).orElse(null);
    }
}
