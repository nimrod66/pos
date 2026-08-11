package com.example.pos.prescriptions.prescriptions.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionRequestDto;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionResponseDto;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.prescriptions.prescriptions.repository.PrescriptionsRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PrescriptionsService {

    private final PrescriptionsRepository repo;
    private final MedicineRepository medicineRepo;
    private final AuthenticatedUserContext current;

    public PrescriptionsService(PrescriptionsRepository repo,
                                MedicineRepository medicineRepo,
                                AuthenticatedUserContext current) {
        this.repo = repo;
        this.medicineRepo = medicineRepo;
        this.current = current;
    }

    public Prescriptions create(PrescriptionRequestDto dto) {
        User approver = current.user();
        Branch branch = approver.getBranch();
        String prescriptionNumber = dto.getPrescriptionNumber().trim();
        if (repo.existsByBranchIdAndPrescriptionNumberIgnoreCase(branch.getId(), prescriptionNumber)) {
            throw new ConflictException("Prescription number already exists in this branch",
                    "PRESCRIPTION_NUMBER_EXISTS");
        }
        if (dto.getIssuedDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Prescription issue date cannot be in the future",
                    "INVALID_PRESCRIPTION_DATE");
        }

        LocalDateTime now = LocalDateTime.now();
        Prescriptions prescription = Prescriptions.builder()
                .branch(branch)
                .approvedBy(approver)
                .customerName(dto.getCustomerName().trim())
                .doctorName(dto.getDoctorName().trim())
                .doctorLicenseNumber(dto.getDoctorLicenseNumber().trim())
                .hospitalName(trimToNull(dto.getHospitalName()))
                .prescriptionNumber(prescriptionNumber)
                .diagnosis(trimToNull(dto.getDiagnosis()))
                .issuedDate(dto.getIssuedDate())
                .status("ACTIVE")
                .approvedAt(now)
                .build();

        Set<UUID> medicineIds = new HashSet<>();
        for (PrescriptionRequestDto.PrescriptionItemDto item : dto.getItems()) {
            if (!medicineIds.add(item.getMedicineId())) {
                throw new BadRequestException("A medicine may appear only once on a prescription",
                        "DUPLICATE_PRESCRIPTION_MEDICINE");
            }
            Medicine medicine = medicineRepo.findByIdAndPharmacyId(
                            item.getMedicineId(), branch.getPharmacy().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", item.getMedicineId()));
            prescription.getPrescriptionItems().add(PrescriptionItems.builder()
                    .prescriptions(prescription)
                    .medicine(medicine)
                    .dosage(trimToNull(item.getDosage()))
                    .quantity(item.getQuantity())
                    .build());
        }
        return repo.saveAndFlush(prescription);
    }

    @Transactional(readOnly = true)
    public Page<Prescriptions> getAll(Pageable pageable) {
        return repo.findByBranchId(current.branch().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Prescriptions getById(UUID id) {
        return repo.findDetailedByIdAndBranchId(id, current.branch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
    }

    public Prescriptions dispense(UUID id) {
        Prescriptions prescription = getById(id);
        if (!"ACTIVE".equalsIgnoreCase(prescription.getStatus())) {
            throw new ConflictException("Prescription is not active", "PRESCRIPTION_NOT_ACTIVE");
        }
        prescription.setStatus("DISPENSED");
        prescription.setDispensedAt(LocalDateTime.now());
        return repo.save(prescription);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponseDto toDto(Prescriptions prescription) {
        Prescriptions detailed = getById(prescription.getId());
        PrescriptionResponseDto dto = PrescriptionResponseDto.from(detailed);
        List<PrescriptionResponseDto.PrescriptionItemResponse> items = detailed.getPrescriptionItems().stream()
                .map(item -> PrescriptionResponseDto.PrescriptionItemResponse.builder()
                        .id(item.getId())
                        .medicineId(item.getMedicine().getId())
                        .medicineName(item.getMedicine().getBrandName())
                        .dosage(item.getDosage())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        dto.setItems(items);
        return dto;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
