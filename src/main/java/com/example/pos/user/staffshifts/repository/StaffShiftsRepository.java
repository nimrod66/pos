package com.example.pos.user.staffshifts.repository;

import java.util.UUID;

import com.example.pos.user.staffshifts.model.StaffShifts;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffShiftsRepository extends JpaRepository<StaffShifts, UUID> {

    List<StaffShifts> findByBranchId(UUID branchId);

    List<StaffShifts> findByUserId(UUID userId);

    List<StaffShifts> findByBranchIdAndUserId(UUID branchId, UUID userId);

    Optional<StaffShifts> findTopByUserIdOrderByShiftStartTimeDesc(UUID userId);

    Optional<StaffShifts> findByUserIdAndStatus(UUID userId, StaffShifts.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from StaffShifts shift join fetch shift.user join fetch shift.branch "
            + "where shift.id = :id")
    Optional<StaffShifts> findForUpdateById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shift from StaffShifts shift join fetch shift.user join fetch shift.branch "
            + "where shift.user.id = :userId and shift.status = :status")
    Optional<StaffShifts> findForUpdateByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") StaffShifts.Status status);

    List<StaffShifts> findByBranchIdAndShiftStartTimeBetween(
            UUID branchId, LocalDateTime start, LocalDateTime end);

    List<StaffShifts> findByStatus(StaffShifts.Status status);

    boolean existsByUserIdAndStatus(UUID userId, StaffShifts.Status status);
}
