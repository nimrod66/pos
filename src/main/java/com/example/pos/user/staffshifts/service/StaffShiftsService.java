package com.example.pos.user.staffshifts.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.roles.repository.UserRolesRepository;
import com.example.pos.user.staffshifts.dto.StaffShiftRequestDto;
import com.example.pos.user.staffshifts.dto.UpdateShiftStatusDto;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StaffShiftsService {

    private final StaffShiftsRepository shiftRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final UserRolesRepository rolesRepository;

    public StaffShiftsService(StaffShiftsRepository shiftRepository,
                              BranchRepository branchRepository,
                              UserRepository userRepository,
                              UserRolesRepository rolesRepository) {
        this.shiftRepository = shiftRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.rolesRepository = rolesRepository;
    }

    public StaffShifts openShift(StaffShiftRequestDto dto) {
        if (shiftRepository.existsByUserIdAndStatus(dto.getUserId(), StaffShifts.Status.ACTIVE)) {
            throw new ConflictException("User already has an active shift");
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        StaffShifts shift = new StaffShifts();
        shift.setBranch(branch);
        shift.setUser(user);

        if (dto.getRoleId() != null) {
            UserRoles role = rolesRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("UserRoles", dto.getRoleId()));
            shift.setUserRoles(role);
        }

        shift.setShiftName(dto.getShiftName());
        shift.setShiftNumber(dto.getShiftNumber());
        shift.setShiftStartTime(
                dto.getShiftStartTime() != null ? dto.getShiftStartTime() : LocalDateTime.now());
        shift.setShiftEndTime(dto.getShiftEndTime());
        shift.setRemarks(dto.getRemarks());
        shift.setStatus(StaffShifts.Status.ACTIVE);

        return shiftRepository.save(shift);
    }

    public StaffShifts closeShift(UUID id, UpdateShiftStatusDto dto) {
        StaffShifts shift = getShiftById(id);
        if (shift.getStatus() != StaffShifts.Status.ACTIVE) {
            throw new BadRequestException("Only active shifts can be closed");
        }
        shift.setStatus(StaffShifts.Status.CLOSED);
        shift.setShiftEndTime(LocalDateTime.now());
        if (dto.getRemarks() != null) {
            shift.setRemarks(shift.getRemarks() != null
                    ? shift.getRemarks() + "; " + dto.getRemarks()
                    : dto.getRemarks());
        }
        return shiftRepository.save(shift);
    }

    public StaffShifts cancelShift(UUID id, UpdateShiftStatusDto dto) {
        StaffShifts shift = getShiftById(id);
        if (shift.getStatus() != StaffShifts.Status.ACTIVE) {
            throw new BadRequestException("Only active shifts can be cancelled");
        }
        shift.setStatus(StaffShifts.Status.CANCELLED);
        shift.setShiftEndTime(LocalDateTime.now());
        if (dto.getRemarks() != null) {
            shift.setRemarks(shift.getRemarks() != null
                    ? shift.getRemarks() + "; " + dto.getRemarks()
                    : dto.getRemarks());
        }
        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getShiftsByBranch(UUID branchId) {
        return shiftRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getShiftsByUser(UUID userId) {
        return shiftRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<StaffShifts> getActiveShifts(UUID branchId) {
        if (branchId != null) {
            return shiftRepository.findByBranchId(branchId).stream()
                    .filter(s -> s.getStatus() == StaffShifts.Status.ACTIVE)
                    .toList();
        }
        return shiftRepository.findByStatus(StaffShifts.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public StaffShifts getShiftById(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", id));
    }

    @Transactional(readOnly = true)
    public StaffShifts getActiveShiftForUser(UUID userId) {
        return shiftRepository.findByUserIdAndStatus(userId, StaffShifts.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active shift found for user " + userId));
    }
}
