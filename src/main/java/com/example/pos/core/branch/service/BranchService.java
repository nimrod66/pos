package com.example.pos.core.branch.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.dto.BranchRequestDto;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuthenticatedUserContext current;

    public BranchService(BranchRepository branchRepository,
                         AuthenticatedUserContext current) {
        this.branchRepository = branchRepository;
        this.current = current;
    }

    public Branch createBranch(BranchRequestDto dto) {
        current.requirePharmacy(dto.getPharmacyId());
        String code = normalizeCode(dto.getBranchCode());
        if (branchRepository.existsByPharmacyIdAndBranchCodeIgnoreCase(
                current.pharmacy().getId(), code)) {
            throw new ConflictException("Branch code " + code + " already exists");
        }
        Branch branch = new Branch();
        branch.setPharmacy(current.pharmacy());
        mapToEntity(dto, branch);
        branch.setStatus(Branch.Status.ACTIVE);
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findByPharmacyId(current.pharmacy().getId());
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesByPharmacyId(UUID pharmacyId) {
        current.requirePharmacy(pharmacyId);
        return branchRepository.findByPharmacyId(pharmacyId);
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(UUID id) {
        return branchRepository.findByIdAndPharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    public Branch updateBranch(UUID id, BranchRequestDto dto) {
        Branch branch = getBranchById(id);
        current.requirePharmacy(dto.getPharmacyId());
        String code = normalizeCode(dto.getBranchCode());
        if (branchRepository.existsByPharmacyIdAndBranchCodeIgnoreCaseAndIdNot(
                current.pharmacy().getId(), code, id)) {
            throw new ConflictException("Branch code " + code + " already exists");
        }
        if (dto.getEmail() != null && branchRepository.existsByPharmacyIdAndEmailIgnoreCaseAndIdNot(
                current.pharmacy().getId(), dto.getEmail().trim(), id)) {
            throw new ConflictException("Email " + dto.getEmail() + " is already in use");
        }
        mapToEntity(dto, branch);
        return branchRepository.save(branch);
    }

    public void deleteBranch(UUID id) {
        Branch branch = getBranchById(id);
        if (current.branch().getId().equals(id)) {
            throw new ForbiddenException("You cannot deactivate the active session branch");
        }
        branch.setStatus(Branch.Status.INACTIVE);
        branchRepository.save(branch);
    }

    private void mapToEntity(BranchRequestDto dto, Branch branch) {
        branch.setBranchName(dto.getBranchName().trim());
        branch.setBranchCode(normalizeCode(dto.getBranchCode()));
        branch.setPhoneNumber(dto.getPhoneNumber().trim());
        branch.setEmail(dto.getEmail() == null ? null : dto.getEmail().trim().toLowerCase(Locale.ROOT));
        branch.setLocation(dto.getLocation().trim());
        if (dto.getStatus() != null) {
            try {
                branch.setStatus(Branch.Status.valueOf(dto.getStatus().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid branch status: " + dto.getStatus());
            }
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
