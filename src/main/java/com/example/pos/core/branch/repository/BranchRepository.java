package com.example.pos.core.branch.repository;

import java.util.UUID;

import com.example.pos.core.branch.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndIdNot(String branchCode, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<Branch> findByPharmacyId(UUID pharmacyId);
}
