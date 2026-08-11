package com.example.pos.core.branch.repository;

import java.util.UUID;

import com.example.pos.core.branch.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchCodeAndIdNot(String branchCode, UUID id);

    boolean existsByPharmacyIdAndBranchCodeIgnoreCase(UUID pharmacyId, String branchCode);

    boolean existsByPharmacyIdAndBranchCodeIgnoreCaseAndIdNot(
            UUID pharmacyId, String branchCode, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByPharmacyIdAndEmailIgnoreCaseAndIdNot(UUID pharmacyId, String email, UUID id);

    List<Branch> findByPharmacyId(UUID pharmacyId);

    Optional<Branch> findByIdAndPharmacyId(UUID id, UUID pharmacyId);
}
