package com.example.pos.core.branch.repository;

import com.example.pos.core.branch.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndIdNot(String branchCode, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Branch> findByPharmacyId(Long pharmacyId);
}
