package com.example.pos.user.staffshifts.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.systemsettings.SettingKeys;
import com.example.pos.core.systemsettings.service.SystemSettingsService;
import com.example.pos.notification.model.Notification;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.security.auth.PermissionCodes;
import com.example.pos.user.staffshifts.dto.StaffShiftRequestDto;
import com.example.pos.user.staffshifts.dto.StaffShiftResponseDto;
import com.example.pos.user.staffshifts.dto.UpdateShiftStatusDto;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import com.example.pos.user.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StaffShiftsService {

    private static final Logger log = LoggerFactory.getLogger(StaffShiftsService.class);

    private final StaffShiftsRepository shiftRepository;
    private final CashDrawersRepository drawerRepository;
    private final PaymentRepository paymentRepository;
    private final CashTransactionsRepository cashTransactionsRepository;
    private final BranchRepository branchRepository;
    private final com.example.pos.notification.repository.NotificationRepository notificationRepository;
    private final SystemSettingsService settingsService;
    private final AuthenticatedUserContext current;

    public StaffShiftsService(StaffShiftsRepository shiftRepository,
                              CashDrawersRepository drawerRepository,
                              PaymentRepository paymentRepository,
                              CashTransactionsRepository cashTransactionsRepository,
                              BranchRepository branchRepository,
                              com.example.pos.notification.repository.NotificationRepository notificationRepository,
                              SystemSettingsService settingsService,
                              AuthenticatedUserContext current) {
        this.shiftRepository = shiftRepository;
        this.drawerRepository = drawerRepository;
        this.paymentRepository = paymentRepository;
        this.cashTransactionsRepository = cashTransactionsRepository;
        this.branchRepository = branchRepository;
        this.notificationRepository = notificationRepository;
        this.settingsService = settingsService;
        this.current = current;
    }

    @Auditable(action = "OPEN_SHIFT", entity = "StaffShift")
    public StaffShifts openShift(StaffShiftRequestDto dto) {
        User user = current.user();
        Branch branch = current.branch();
        rejectSpoofedScope(dto.getUserId(), dto.getBranchId(), user, branch);

        if (shiftRepository.findForUpdateByUserIdAndStatus(
                user.getId(), StaffShifts.Status.ACTIVE).isPresent()) {
            throw new ConflictException("User already has an active shift", "SHIFT_ALREADY_OPEN");
        }

        LocalDateTime now = LocalDateTime.now();
        StaffShifts shift = StaffShifts.builder()
                .branch(branch)
                .user(user)
                .shiftName(dto.getShiftName() == null || dto.getShiftName().isBlank()
                        ? "Till shift" : dto.getShiftName().trim())
                .shiftNumber(dto.getShiftNumber())
                .shiftStartTime(now)
                .remarks(trimToNull(dto.getRemarks()))
                .status(StaffShifts.Status.ACTIVE)
                .build();
        BigDecimal openingFloat = dto.getOpeningFloat() != null
                ? dto.getOpeningFloat() : BigDecimal.ZERO;
        if (openingFloat.signum() < 0) {
            throw new BadRequestException("Opening float cannot be negative", "INVALID_OPENING_FLOAT");
        }
        try {
            shift = shiftRepository.saveAndFlush(shift);
            drawerRepository.saveAndFlush(CashDrawers.builder()
                    .staffShifts(shift)
                    .openingBalance(openingFloat)
                    .expectedClosingBalance(openingFloat)
                    .openingTime(LocalTime.now())
                    .status("OPEN")
                    .build());
            notifyBranch(branch.getId(), Notification.Type.SHIFT_REMINDER,
                    "Shift opened",
                    user.getFirstName() + " " + user.getLastName()
                            + " opened a till at " + branch.getBranchName()
                            + " (float KES " + openingFloat.toPlainString() + ").");
            return shift;
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("User already has an active shift", "SHIFT_ALREADY_OPEN");
        }
    }

    @Auditable(action = "CLOSE_SHIFT", entity = "StaffShift")
    public StaffShifts closeShift(UUID id, UpdateShiftStatusDto dto) {
        StaffShifts shift = getScopedShiftForUpdate(id);
        if (shift.getStatus() != StaffShifts.Status.ACTIVE) {
            throw new BadRequestException("Only active shifts can be closed", "SHIFT_NOT_OPEN");
        }
        if (dto.getActualCash() == null || dto.getActualCash().signum() < 0) {
            throw new BadRequestException("Actual cash is required to close a shift",
                    "ACTUAL_CASH_REQUIRED");
        }

        LocalDateTime now = LocalDateTime.now();
        CashDrawers drawer = drawerRepository.findOpenForUpdateByShiftId(id)
                .orElseThrow(() -> new ConflictException("The shift has no open cash drawer",
                        "CASH_DRAWER_NOT_OPEN"));
        BigDecimal cashSales = paymentRepository.sumCompletedCashForShift(id);
        BigDecimal expected = drawer.getOpeningBalance()
                .add(cashSales)
                .add(cashTransactionsRepository.sumNetCashForDrawer(drawer.getId()));
        drawer.setExpectedClosingBalance(expected);
        drawer.setActualClosingBalance(dto.getActualCash());
        drawer.setVariance(dto.getActualCash().subtract(expected));
        drawer.setClosingTime(LocalTime.now());
        drawer.setStatus("CLOSED");
        drawerRepository.save(drawer);

        shift.setStatus(StaffShifts.Status.CLOSED);
        shift.setShiftEndTime(now);
        appendRemarks(shift, dto.getRemarks());
        StaffShifts saved = shiftRepository.save(shift);

        BigDecimal variance = drawer.getVariance() == null ? BigDecimal.ZERO : drawer.getVariance();
        boolean hasVariance = variance.abs().compareTo(BigDecimal.valueOf(0.01)) > 0;
        notifyBranch(shift.getBranch().getId(),
                hasVariance ? Notification.Type.SYSTEM_ALERT : Notification.Type.SHIFT_REMINDER,
                hasVariance ? "Shift closed with cash variance" : "Shift closed",
                shift.getUser().getFirstName() + " " + shift.getUser().getLastName()
                        + " closed their till at " + shift.getBranch().getBranchName()
                        + ". Expected KES " + expected.toPlainString()
                        + ", counted KES " + dto.getActualCash().toPlainString()
                        + (hasVariance ? ", variance KES " + variance.toPlainString() + "."
                                       : " - reconciled."));
        return saved;
    }

    @Auditable(action = "CANCEL_SHIFT", entity = "StaffShift")
    public StaffShifts cancelShift(UUID id, UpdateShiftStatusDto dto) {
        if (!current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException("Cancelling a shift requires variance approval permission");
        }
        StaffShifts shift = getBranchShiftForUpdate(id);
        if (shift.getStatus() != StaffShifts.Status.ACTIVE) {
            throw new BadRequestException("Only active shifts can be cancelled", "SHIFT_NOT_OPEN");
        }
        shift.setStatus(StaffShifts.Status.CANCELLED);
        shift.setShiftEndTime(LocalDateTime.now());
        appendRemarks(shift, dto.getRemarks());
        drawerRepository.findByStaffShiftsId(id).stream()
                .filter(value -> "OPEN".equals(value.getStatus()))
                .forEach(drawer -> {
                    drawer.setClosingTime(LocalTime.now());
                    drawer.setStatus("CANCELLED");
                    drawerRepository.save(drawer);
                });
        return shiftRepository.save(shift);
    }

    /** Owner sign-off on a closed shift's cash variance - appends a review note. */
    public StaffShifts reviewVariance(UUID id, UpdateShiftStatusDto dto) {
        if (!current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException(
                    "Reviewing variances requires the variance approval permission");
        }
        StaffShifts shift = getBranchShiftForUpdate(id);
        if (shift.getStatus() == StaffShifts.Status.ACTIVE) {
            throw new BadRequestException(
                    "Variances can only be reviewed after the shift is closed",
                    "SHIFT_NOT_CLOSED");
        }
        appendRemarks(shift, "Variance reviewed"
                + (trimToNull(dto.getRemarks()) == null ? "" : ": " + trimToNull(dto.getRemarks())));
        shift.setStatus(StaffShifts.Status.REVIEWED);
        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getShiftsByBranch(UUID branchId) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return shiftRepository.findByBranchId(branch.getId());
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getShiftsByUser(UUID userId) {
        User user = current.user();
        if (!user.getId().equals(userId)
                && !current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException("Another user's shifts are not accessible");
        }
        return shiftRepository.findByBranchIdAndUserId(current.branch().getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getActiveShifts(UUID branchId) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return shiftRepository.findByBranchId(branch.getId()).stream()
                .filter(shift -> shift.getStatus() == StaffShifts.Status.ACTIVE)
                .filter(shift -> current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)
                        || shift.getUser().getId().equals(current.userId()))
                .toList();
    }

    /**
     * Reconciliation view for owners: the most recent shifts across every
     * branch of the pharmacy, or one specific branch. Requires variance
     * approval permission.
     */
    @Transactional(readOnly = true)
    public List<StaffShifts> getShiftHistory(UUID branchId) {
        if (!current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException(
                    "Shift reconciliation requires the variance approval permission");
        }
        UUID pharmacyId = current.pharmacy().getId();
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
            if (branch.getPharmacy() == null
                    || !pharmacyId.equals(branch.getPharmacy().getId())) {
                throw new ForbiddenException("That branch belongs to another pharmacy");
            }
            return shiftRepository.findHistoryForBranch(pharmacyId, branchId);
        }
        return shiftRepository.findHistoryForPharmacy(pharmacyId);
    }

    @Transactional(readOnly = true)
    public StaffShifts getShiftById(UUID id) {
        return getScopedShift(id);
    }

    @Transactional(readOnly = true)
    public StaffShifts getActiveShiftForUser(UUID userId) {
        User user = current.user();
        if (!user.getId().equals(userId)
                && !current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException("Another user's shift is not accessible");
        }
        return shiftRepository.findByUserIdAndStatus(userId, StaffShifts.Status.ACTIVE)
                .filter(shift -> shift.getBranch().getId().equals(current.branch().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No active shift found for user " + userId));
    }

    @Transactional(readOnly = true)
    public StaffShiftResponseDto toResponse(StaffShifts shift) {
        CashDrawers drawer = drawerRepository.findByStaffShiftsId(shift.getId())
                .stream()
                .findFirst()
                .orElse(null);
        BigDecimal openingFloat = drawer != null && drawer.getOpeningBalance() != null
                ? drawer.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal cashSales = paymentRepository.sumCompletedCashForShift(shift.getId());
        BigDecimal mpesaSales = paymentRepository.sumCompletedMpesaForShift(shift.getId());
        BigDecimal netDrawerTransactions = drawer == null
                ? BigDecimal.ZERO
                : cashTransactionsRepository.sumNetCashForDrawer(drawer.getId());
        BigDecimal expectedCash = openingFloat.add(cashSales).add(netDrawerTransactions);
        BigDecimal cashRefunds = netDrawerTransactions.signum() < 0
                ? netDrawerTransactions.abs() : BigDecimal.ZERO;
        BigDecimal actualCash = drawer != null ? drawer.getActualClosingBalance() : null;
        BigDecimal variance = drawer != null ? drawer.getVariance() : null;
        return StaffShiftResponseDto.from(
                shift,
                openingFloat,
                cashSales,
                mpesaSales,
                cashRefunds,
                expectedCash,
                actualCash,
                variance);
    }

    /**
     * Auto-close shifts that have been open past the configured hour.
     * Runs every 5 minutes. Reads shift.auto_close_hour from system settings per branch.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    @Transactional
    public void autoCloseExpiredShifts() {
        int closeHour;
        try {
            closeHour = Integer.parseInt(
                    settingsService.resolveSettingValue(SettingKeys.Shift.AUTO_CLOSE_HOUR, null, null, "23"));
        } catch (NumberFormatException e) {
            closeHour = 23;
        }
        LocalTime threshold = LocalTime.of(closeHour, 0);
        LocalDateTime now = LocalDateTime.now();

        List<StaffShifts> activeShifts = shiftRepository.findByStatus(StaffShifts.Status.ACTIVE);
        for (StaffShifts shift : activeShifts) {
            if (shift.getShiftStartTime() == null) continue;
            LocalTime openedTime = shift.getShiftStartTime().toLocalTime();
            boolean pastThreshold = openedTime.isBefore(threshold) && now.toLocalTime().isAfter(threshold);
            boolean openPastMidnight = openedTime.isAfter(threshold) && now.toLocalTime().isBefore(threshold)
                    && shift.getShiftStartTime().toLocalDate().isBefore(now.toLocalDate());

            if (pastThreshold || openPastMidnight) {
                log.info("Auto-closing shift {} (opened at {}) past hour {}",
                        shift.getId(), shift.getShiftStartTime(), closeHour);
                try {
                    CashDrawers drawer = drawerRepository.findByStaffShiftsId(shift.getId())
                            .stream().filter(d -> "OPEN".equals(d.getStatus()))
                            .findFirst().orElse(null);
                    BigDecimal expectedCash = BigDecimal.ZERO;
                    if (drawer != null) {
                        BigDecimal cashSales = paymentRepository.sumCompletedCashForShift(shift.getId());
                        expectedCash = drawer.getOpeningBalance()
                                .add(cashSales)
                                .add(cashTransactionsRepository.sumNetCashForDrawer(drawer.getId()));
                        drawer.setExpectedClosingBalance(expectedCash);
                        drawer.setActualClosingBalance(expectedCash);
                        drawer.setVariance(BigDecimal.ZERO);
                        drawer.setClosingTime(LocalTime.now());
                        drawer.setStatus("CLOSED");
                        drawerRepository.save(drawer);
                    }
                    shift.setStatus(StaffShifts.Status.CLOSED);
                    shift.setShiftEndTime(now);
                    appendRemarks(shift, "Auto-closed at " + now + " (scheduled)");
                    shiftRepository.save(shift);
                    notifyBranch(shift.getBranch().getId(),
                            Notification.Type.SHIFT_REMINDER,
                            "Shift auto-closed",
                            "Shift opened by " + shift.getUser().getFirstName()
                                    + " " + shift.getUser().getLastName()
                                    + " was auto-closed at " + shift.getBranch().getBranchName()
                                    + " past the configured hour (" + closeHour + ":00).");
                } catch (Exception e) {
                    log.error("Failed to auto-close shift {}: {}", shift.getId(), e.getMessage());
                }
            }
        }
    }

    private StaffShifts getScopedShift(UUID id) {
        StaffShifts shift = getBranchShift(id);
        if (!shift.getUser().getId().equals(current.userId())) {
            throw new ForbiddenException("The shift belongs to another user");
        }
        return shift;
    }

    private StaffShifts getScopedShiftForUpdate(UUID id) {
        StaffShifts shift = getBranchShiftForUpdate(id);
        if (!shift.getUser().getId().equals(current.userId())) {
            throw new ForbiddenException("The shift belongs to another user");
        }
        return shift;
    }

    private StaffShifts getBranchShift(UUID id) {
        StaffShifts shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", id));
        current.requireBranch(shift.getBranch().getId());
        return shift;
    }

    private StaffShifts getBranchShiftForUpdate(UUID id) {
        StaffShifts shift = shiftRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", id));
        current.requireBranch(shift.getBranch().getId());
        return shift;
    }

    private void rejectSpoofedScope(UUID requestedUserId, UUID requestedBranchId,
                                    User user, Branch branch) {
        if (requestedUserId != null && !user.getId().equals(requestedUserId)) {
            throw new ForbiddenException("A shift cannot be opened for another user");
        }
        if (requestedBranchId != null && !branch.getId().equals(requestedBranchId)) {
            throw new ForbiddenException("A shift cannot be opened for another branch");
        }
    }

    private void appendRemarks(StaffShifts shift, String remarks) {
        String clean = trimToNull(remarks);
        if (clean == null) return;
        shift.setRemarks(shift.getRemarks() == null ? clean : shift.getRemarks() + "; " + clean);
    }

    private void notifyBranch(UUID branchId, Notification.Type type,
                              String title, String message) {
        try {
            notificationRepository.save(Notification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .status(Notification.Status.UNREAD)
                    .branchId(branchId)
                    .referenceType("STAFF_SHIFT")
                    .build());
        } catch (Exception ignored) {
            // A failed bell notification must never block shift operations.
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
