package com.example.pos.prescriptions.prescriptions.repository;

import java.util.UUID;

import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface PrescriptionsRepository extends JpaRepository<Prescriptions, UUID> {

    Page<Prescriptions> findByBranchId(UUID branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "approvedBy", "prescriptionItems", "prescriptionItems.medicine"})
    Optional<Prescriptions> findDetailedByIdAndBranchId(UUID id, UUID branchId);

    boolean existsByBranchIdAndPrescriptionNumberIgnoreCase(UUID branchId, String prescriptionNumber);

    Optional<Prescriptions> findByPrescriptionNumber(String number);

    @EntityGraph(attributePaths = {"prescriptionItems", "prescriptionItems.medicine"})
    List<Prescriptions> findByCustomerNameContainingIgnoreCaseAndBranchId(String customerName, UUID branchId);
}
