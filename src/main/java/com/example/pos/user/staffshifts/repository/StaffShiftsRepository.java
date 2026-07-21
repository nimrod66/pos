package com.example.pos.user.staffshifts.repository;

import com.example.pos.user.staffshifts.model.StaffShifts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffShiftsRepository extends JpaRepository<StaffShifts, Long> {

    List<StaffShifts> findByBranchId(Long branchId);

    List<StaffShifts> findByUserId(Long userId);

    List<StaffShifts> findByBranchIdAndUserId(Long branchId, Long userId);

    Optional<StaffShifts> findTopByUserIdOrderByShiftStartTimeDesc(Long userId);

    Optional<StaffShifts> findByUserIdAndStatus(Long userId, StaffShifts.Status status);

    List<StaffShifts> findByBranchIdAndShiftStartTimeBetween(
            Long branchId, LocalDateTime start, LocalDateTime end);

    List<StaffShifts> findByStatus(StaffShifts.Status status);

    boolean existsByUserIdAndStatus(Long userId, StaffShifts.Status status);
}
