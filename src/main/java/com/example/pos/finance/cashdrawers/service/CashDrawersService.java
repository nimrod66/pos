package com.example.pos.finance.cashdrawers.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.finance.cashdrawers.dto.CashDrawerRequestDto;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.security.auth.PermissionCodes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CashDrawersService {

    private final CashDrawersRepository repo;
    private final StaffShiftsRepository shiftRepo;
    private final AuthenticatedUserContext current;

    public CashDrawersService(CashDrawersRepository repo, StaffShiftsRepository shiftRepo,
                              AuthenticatedUserContext current) {
        this.repo = repo;
        this.shiftRepo = shiftRepo;
        this.current = current;
    }

    public CashDrawers openDrawer(CashDrawerRequestDto dto) {
        throw new BadRequestException("Cash drawers are opened with a staff shift",
                "DIRECT_DRAWER_OPEN_DISABLED");
    }

    public CashDrawers closeDrawer(UUID id, CashDrawerRequestDto dto) {
        throw new BadRequestException("Cash drawers are closed with a staff shift",
                "DIRECT_DRAWER_CLOSE_DISABLED");
    }

    @Transactional(readOnly = true)
    public Page<CashDrawers> getByShift(UUID shiftId, Pageable pageable) {
        StaffShifts shift = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", shiftId));
        current.requireBranch(shift.getBranch().getId());
        requireOwnerOrApprover(shift);
        List<CashDrawers> list = repo.findByStaffShiftsIdAndStaffShiftsBranchId(
                shiftId, current.branchId());
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public CashDrawers getById(UUID id) {
        CashDrawers drawer = repo.findByIdAndStaffShiftsBranchId(id, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("CashDrawer", id));
        requireOwnerOrApprover(drawer.getStaffShifts());
        return drawer;
    }

    private void requireOwnerOrApprover(StaffShifts shift) {
        if (!shift.getUser().getId().equals(current.userId())
                && !current.hasAuthority(PermissionCodes.SHIFT_VARIANCE_APPROVE)) {
            throw new ForbiddenException("Another user's cash drawer is not accessible");
        }
    }
}
