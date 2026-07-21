package com.example.pos.finance.cashdrawers.repository;

import com.example.pos.finance.cashdrawers.model.CashDrawers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashDrawersRepository extends JpaRepository<CashDrawers, Long> {

    List<CashDrawers> findByStaffShiftsId(Long shiftId);
}
