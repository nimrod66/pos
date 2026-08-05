package com.example.pos.user.staffshifts.repository;

import java.util.UUID;

import com.example.pos.user.staffshifts.model.StaffShifts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffShiftsRepository extends JpaRepository<StaffShifts, UUID> {

    List<StaffShifts> findByBranchId(UUID branchId);

    List<StaffShifts> findByUserId(UUID userId);

    List<StaffShifts> findByBranchIdAndUserId(UUID branchId, UUID userId);

    Optional<StaffShifts> findTopByUserIdOrderByShiftStartTimeDesc(UUID userId);

    Optional<StaffShifts> findByUserIdAndStatus(UUID userId, StaffShifts.Status status);

    List<StaffShifts> findByBranchIdAndShiftStartTimeBetween(
            UUID branchId, LocalDateTime start, LocalDateTime end);

    List<StaffShifts> findByStatus(StaffShifts.Status status);

    boolean existsByUserIdAndStatus(UUID userId, StaffShifts.Status status);
}
