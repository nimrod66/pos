package com.example.pos.prescriptions.prescriptions.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionRequestDto;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionResponseDto;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.prescriptions.prescriptions.repository.PrescriptionsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PrescriptionsService {

    private final PrescriptionsRepository repo;
    private final MedicineRepository medicineRepo;

    public PrescriptionsService(PrescriptionsRepository repo, MedicineRepository medicineRepo) {
        this.repo = repo;
        this.medicineRepo = medicineRepo;
    }

    public Prescriptions create(PrescriptionRequestDto dto) {
        Prescriptions p = new Prescriptions();
        p.setCustomerName(dto.getCustomerName());
        p.setDoctorName(dto.getDoctorName());
        p.setDoctorLicenseNumber(dto.getDoctorLicenseNumber());
        p.setHospitalName(dto.getHospitalName());
        p.setPrescriptionNumber(dto.getPrescriptionNumber());
        p.setDiagnosis(dto.getDiagnosis());
        p.setIssuedDate(dto.getIssuedDate() != null ? dto.getIssuedDate() : LocalDate.now());
        p.setStatus("ACTIVE");
        repo.save(p);

        List<PrescriptionItems> items = new ArrayList<>();
        for (PrescriptionRequestDto.PrescriptionItemDto item : dto.getItems()) {
            Medicine medicine = medicineRepo.findById(item.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", item.getMedicineId()));
            PrescriptionItems pi = new PrescriptionItems();
            pi.setPrescriptions(p);
            pi.setMedicine(medicine);
            pi.setDosage(item.getDosage());
            pi.setQuantity(item.getQuantity());
            items.add(pi);
        }
        return p;
    }

    @Transactional(readOnly = true)
    public Page<Prescriptions> getAll(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional(readOnly = true)
    public Prescriptions getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
    }

    public Prescriptions dispense(Long id) {
        Prescriptions p = getById(id);
        p.setStatus("DISPENSED");
        return repo.save(p);
    }

    public PrescriptionResponseDto toDto(Prescriptions p) {
        PrescriptionResponseDto dto = PrescriptionResponseDto.from(p);
        if (p.getPrescriptionItems() != null) {
            List<PrescriptionResponseDto.PrescriptionItemResponse> items = p.getPrescriptionItems().stream()
                    .map(pi -> PrescriptionResponseDto.PrescriptionItemResponse.builder()
                            .id(pi.getId()).medicineId(pi.getMedicine() != null ? pi.getMedicine().getId() : null)
                            .medicineName(pi.getMedicine() != null ? pi.getMedicine().getBrandName() : null)
                            .dosage(pi.getDosage()).quantity(pi.getQuantity()).build())
                    .toList();
            dto.setItems(items);
        }
        return dto;
    }
}
