package com.example.pos.finance.cashdrawers.repository;

import java.util.UUID;

import com.example.pos.finance.cashdrawers.model.CashDrawers;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface CashDrawersRepository extends JpaRepository<CashDrawers, UUID> {

    List<CashDrawers> findByStaffShiftsId(UUID shiftId);

    @EntityGraph(attributePaths = {"staffShifts", "staffShifts.branch", "staffShifts.user"})
    List<CashDrawers> findByStaffShiftsIdAndStaffShiftsBranchId(UUID shiftId, UUID branchId);

    @EntityGraph(attributePaths = {"staffShifts", "staffShifts.branch", "staffShifts.user"})
    Optional<CashDrawers> findByIdAndStaffShiftsBranchId(UUID id, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select drawer from CashDrawers drawer "
            + "where drawer.staffShifts.id = :shiftId and drawer.status = 'OPEN'")
    Optional<CashDrawers> findOpenForUpdateByShiftId(@Param("shiftId") UUID shiftId);
}
