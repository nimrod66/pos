package com.example.pos.finance.cashtransactions.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.finance.cashtransactions.dto.CashTransactionRequestDto;
import com.example.pos.finance.cashtransactions.dto.CashTransactionResponseDto;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CashTransactionsService {

    private final CashTransactionsRepository repo;
    private final CashDrawersRepository drawersRepository;
    private final StaffShiftsRepository shiftRepository;
    private final PaymentRepository paymentRepository;
    private final AuthenticatedUserContext current;

    public CashTransactionsService(CashTransactionsRepository repo,
                                   CashDrawersRepository drawersRepository,
                                   StaffShiftsRepository shiftRepository,
                                   PaymentRepository paymentRepository,
                                   AuthenticatedUserContext current) {
        this.repo = repo;
        this.drawersRepository = drawersRepository;
        this.shiftRepository = shiftRepository;
        this.paymentRepository = paymentRepository;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public Page<CashTransactions> getByCashDrawer(UUID cashDrawerId, Pageable pageable) {
        List<CashTransactions> list = repo.findByCashDrawersIdOrderByIdDesc(cashDrawerId);
        return new PageImpl<>(list, pageable, list.size());
    }

    /** Drawer + transactions for the signed-in user's active shift, if any. */
    @Transactional(readOnly = true)
    public ActiveDrawer activeDrawer() {
        StaffShifts activeShift = shiftRepository
                .findByUserIdAndStatus(current.userId(), StaffShifts.Status.ACTIVE)
                .filter(shift -> shift.getBranch().getId().equals(current.branchId()))
                .orElse(null);
        if (activeShift == null) {
            return null;
        }
        List<CashDrawers> drawers = drawersRepository.findByStaffShiftsId(activeShift.getId());
        if (drawers.isEmpty()) {
            return null;
        }
        CashDrawers drawer = drawers.get(0);
        return new ActiveDrawer(drawer.getId(),
                repo.findByCashDrawersIdOrderByIdDesc(drawer.getId()).stream()
                        .map(CashTransactionResponseDto::from)
                        .toList());
    }

    /** Records a deliberate pay-in / pay-out against the active shift drawer. */
    @Transactional
    public CashTransactionResponseDto recordForActiveShift(CashTransactionRequestDto request) {
        StaffShifts shift = shiftRepository
                .findByUserIdAndStatus(current.userId(), StaffShifts.Status.ACTIVE)
                .orElseThrow(() -> new BadRequestException(
                        "Open a shift before recording cash movements", "NO_ACTIVE_SHIFT"));
        current.requireBranch(shift.getBranch().getId());

        CashDrawers drawer = drawersRepository.findOpenForUpdateByShiftId(shift.getId())
                .orElseThrow(() -> new ConflictException(
                        "The shift has no open cash drawer", "CASH_DRAWER_NOT_OPEN"));

        String type = request.getTransactionType().toUpperCase();
        BigDecimal amount = request.getAmount();

        if ("CASH_OUT".equals(type)) {
            BigDecimal available = drawer.getOpeningBalance()
                    .add(paymentRepository.sumCompletedCashForShift(shift.getId()))
                    .add(repo.sumNetCashForDrawer(drawer.getId()));
            if (available.compareTo(amount) < 0) {
                throw new ConflictException(
                        "The drawer only holds " + available.toPlainString(),
                        "INSUFFICIENT_DRAWER_CASH");
            }
        }

        CashTransactions saved = repo.save(CashTransactions.builder()
                .cashDrawers(drawer)
                .transactionType(type)
                .amount(amount)
                .remarks(request.getRemarks().trim())
                .referenceType("CASH_MANAGEMENT")
                .build());
        return CashTransactionResponseDto.from(saved);
    }

    public record ActiveDrawer(UUID drawerId, List<CashTransactionResponseDto> transactions) {
    }
}
