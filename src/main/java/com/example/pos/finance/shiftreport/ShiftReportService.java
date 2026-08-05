package com.example.pos.finance.shiftreport;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.finance.expenses.repository.ExpensesRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ShiftReportService {

    private final CashDrawersRepository cashDrawerRepo;
    private final StaffShiftsRepository shiftRepo;
    private final SalesRepository salesRepo;
    private final PaymentRepository paymentRepo;
    private final ExpensesRepository expenseRepo;

    public ShiftReportService(CashDrawersRepository cashDrawerRepo,
                               StaffShiftsRepository shiftRepo,
                               SalesRepository salesRepo,
                               PaymentRepository paymentRepo,
                               ExpensesRepository expenseRepo) {
        this.cashDrawerRepo = cashDrawerRepo;
        this.shiftRepo = shiftRepo;
        this.salesRepo = salesRepo;
        this.paymentRepo = paymentRepo;
        this.expenseRepo = expenseRepo;
    }

    public ShiftReport generate(UUID shiftId) {
        StaffShifts shift = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        CashDrawers drawer = cashDrawerRepo.findByStaffShiftsId(shiftId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cash drawer for shift", shiftId));

        List<Sales> shiftSales = salesRepo.findByUserIdAndCreatedAtBetween(
                shift.getUser().getId(), shift.getShiftStartTime(), shift.getShiftEndTime() != null
                        ? shift.getShiftEndTime() : java.time.LocalDateTime.now());

        int salesCount = shiftSales.size();
        BigDecimal totalSales = shiftSales.stream()
                .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = shiftSales.stream()
                .flatMap(s -> s.getPayment().stream())
                .toList();

        BigDecimal cashTotal = sumByMethod(payments, "CASH");
        BigDecimal mpesaTotal = sumByMethod(payments, "M_PESA");
        BigDecimal cardTotal = sumByMethod(payments, "CARD");

        List<Map<String, Object>> paymentBreakdown = List.of(
                Map.of("method", "CASH", "amount", cashTotal, "count", countByMethod(payments, "CASH")),
                Map.of("method", "M_PESA", "amount", mpesaTotal, "count", countByMethod(payments, "M_PESA")),
                Map.of("method", "CARD", "amount", cardTotal, "count", countByMethod(payments, "CARD"))
        );

        List<Expenses> expenses = expenseRepo.findByUserIdAndCreatedAtBetween(
                shift.getUser().getId(), shift.getShiftStartTime(), shift.getShiftEndTime() != null
                        ? shift.getShiftEndTime() : java.time.LocalDateTime.now());

        BigDecimal totalExpenses = expenses.stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> expenseBreakdown = expenses.stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "category", e.getExpenseCategory() != null ? e.getExpenseCategory().getCategoryName() : "-",
                        "amount", e.getAmount(),
                        "description", e.getDescription() != null ? e.getDescription() : ""))
                .toList();

        return new ShiftReport(
                shift.getId(),
                shift.getShiftName(),
                shift.getBranch() != null ? shift.getBranch().getBranchName() : null,
                shift.getUser() != null ? shift.getUser().getFirstName() + " " + shift.getUser().getLastName() : null,
                shift.getShiftStartTime(),
                shift.getShiftEndTime(),
                shift.getStatus() != null ? shift.getStatus().name() : null,
                drawer.getOpeningBalance(),
                drawer.getExpectedClosingBalance(),
                drawer.getActualClosingBalance(),
                drawer.getVariance(),
                salesCount,
                totalSales,
                cashTotal,
                mpesaTotal,
                cardTotal,
                totalExpenses,
                BigDecimal.ZERO,
                0,
                paymentBreakdown,
                expenseBreakdown,
                List.of()
        );
    }

    private BigDecimal sumByMethod(List<Payment> payments, String method) {
        return payments.stream()
                .filter(p -> p.getPaymentMethod() != null && p.getPaymentMethod().name().equalsIgnoreCase(method)
                        && "COMPLETED".equals(p.getPaymentStatus()))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countByMethod(List<Payment> payments, String method) {
        return payments.stream()
                .filter(p -> p.getPaymentMethod() != null && p.getPaymentMethod().name().equalsIgnoreCase(method)
                        && "COMPLETED".equals(p.getPaymentStatus()))
                .count();
    }
}
