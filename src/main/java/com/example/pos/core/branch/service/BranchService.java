package com.example.pos.core.branch.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.dto.BranchRequestDto;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final PharmacyRepository pharmacyRepository;

    public BranchService(BranchRepository branchRepository, PharmacyRepository pharmacyRepository) {
        this.branchRepository = branchRepository;
        this.pharmacyRepository = pharmacyRepository;
    }

    public Branch createBranch(BranchRequestDto dto) {
        Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", dto.getPharmacyId()));

        if (branchRepository.existsByBranchCode(dto.getBranchCode())) {
            throw new ConflictException("Branch code " + dto.getBranchCode() + " already exists");
        }

        Branch branch = new Branch();
        branch.setPharmacy(pharmacy);
        mapToEntity(dto, branch);
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesByPharmacyId(Long pharmacyId) {
        return branchRepository.findByPharmacyId(pharmacyId);
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    public Branch updateBranch(Long id, BranchRequestDto dto) {
        Branch branch = getBranchById(id);

        if (branchRepository.existsByBranchCodeAndIdNot(dto.getBranchCode(), id)) {
            throw new ConflictException("Branch code " + dto.getBranchCode() + " already exists");
        }
        if (dto.getEmail() != null && branchRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ConflictException("Email " + dto.getEmail() + " already in use");
        }

        if (!branch.getPharmacy().getId().equals(dto.getPharmacyId())) {
            Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", dto.getPharmacyId()));
            branch.setPharmacy(pharmacy);
        }

        mapToEntity(dto, branch);
        return branchRepository.save(branch);
    }

    public void deleteBranch(Long id) {
        Branch branch = getBranchById(id);
        branchRepository.delete(branch);
    }

    private void mapToEntity(BranchRequestDto dto, Branch branch) {
        branch.setBranchName(dto.getBranchName());
        branch.setBranchCode(dto.getBranchCode());
        branch.setPhoneNumber(dto.getPhoneNumber());
        branch.setEmail(dto.getEmail());
        branch.setLocation(dto.getLocation());
        if (dto.getStatus() != null) {
            branch.setStatus(Branch.Status.valueOf(dto.getStatus().toUpperCase()));
        }
    }
}
