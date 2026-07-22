package com.example.pos.pharmacy.regulatory.controlledrugs.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.pharmacy.regulatory.controlledrugs.dto.ControlledDrugsRequestDto;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.pharmacy.regulatory.controlledrugs.repository.ControlledDrugsRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.presciptions.prescriptions.model.Prescriptions;
import com.example.pos.presciptions.prescriptions.repository.PrescriptionsRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ControlledDrugsService {

    private final ControlledDrugsRepository repo;
    private final MedicineRepository medicineRepo;
    private final PrescriptionsRepository prescriptionRepo;
    private final UserRepository userRepo;

    public ControlledDrugsService(ControlledDrugsRepository repo, MedicineRepository medicineRepo,
                                  PrescriptionsRepository prescriptionRepo, UserRepository userRepo) {
        this.repo = repo;
        this.medicineRepo = medicineRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.userRepo = userRepo;
    }

    public ControlledDrugs record(ControlledDrugsRequestDto dto) {
        Medicine medicine = medicineRepo.findById(dto.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", dto.getMedicineId()));
        Prescriptions prescription = prescriptionRepo.findById(dto.getPrescriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", dto.getPrescriptionId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        ControlledDrugs cd = new ControlledDrugs();
        cd.setMedicine(medicine);
        cd.setPrescriptions(prescription);
        cd.setUser(user);
        cd.setQuantityDispensed(dto.getQuantityDispensed());
        return repo.save(cd);
    }

    @Transactional(readOnly = true)
    public List<ControlledDrugs> getByMedicine(Long medicineId) {
        return repo.findByMedicineId(medicineId);
    }

    @Transactional(readOnly = true)
    public List<ControlledDrugs> getAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public ControlledDrugs getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("ControlledDrug", id));
    }
}
