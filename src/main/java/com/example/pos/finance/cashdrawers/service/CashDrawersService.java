package com.example.pos.finance.cashdrawers.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.finance.cashdrawers.dto.CashDrawerRequestDto;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class CashDrawersService {

    private final CashDrawersRepository repo;
    private final StaffShiftsRepository shiftRepo;

    public CashDrawersService(CashDrawersRepository repo, StaffShiftsRepository shiftRepo) {
        this.repo = repo;
        this.shiftRepo = shiftRepo;
    }

    public CashDrawers openDrawer(CashDrawerRequestDto dto) {
        StaffShifts shift = shiftRepo.findById(dto.getStaffShiftsId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", dto.getStaffShiftsId()));

        CashDrawers drawer = new CashDrawers();
        drawer.setStaffShifts(shift);
        drawer.setOpeningBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO);
        drawer.setExpectedClosingBalance(dto.getExpectedClosingBalance());
        drawer.setOpeningTime(LocalTime.now());
        drawer.setStatus("OPEN");
        return repo.save(drawer);
    }

    public CashDrawers closeDrawer(Long id, CashDrawerRequestDto dto) {
        CashDrawers drawer = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CashDrawer", id));
        if (!"OPEN".equals(drawer.getStatus())) {
            throw new BadRequestException("Drawer is not open");
        }
        drawer.setActualClosingBalance(dto.getActualClosingBalance());
        drawer.setClosingTime(LocalTime.now());
        drawer.setStatus("CLOSED");
        if (dto.getActualClosingBalance() != null && drawer.getExpectedClosingBalance() != null) {
            drawer.setVariance(dto.getActualClosingBalance().subtract(drawer.getExpectedClosingBalance()));
        }
        return repo.save(drawer);
    }

    @Transactional(readOnly = true)
    public List<CashDrawers> getByShift(Long shiftId) { return repo.findByStaffShiftsId(shiftId); }

    @Transactional(readOnly = true)
    public CashDrawers getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("CashDrawer", id));
    }
}
