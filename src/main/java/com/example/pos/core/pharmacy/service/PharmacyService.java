package com.example.pos.core.pharmacy.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.pharmacy.dto.PharmacyRequestDto;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    public PharmacyService(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    public Pharmacy createPharmacy(PharmacyRequestDto dto) {
        if (pharmacyRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("A pharmacy with email " + dto.getEmail() + " already exists");
        }
        if (pharmacyRepository.existsByLicenseNumber(dto.getLicenseNumber())) {
            throw new ConflictException("A pharmacy with license " + dto.getLicenseNumber() + " already exists");
        }

        Pharmacy pharmacy = new Pharmacy();
        mapToEntity(dto, pharmacy);
        return pharmacyRepository.save(pharmacy);
    }

    @Transactional(readOnly = true)
    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Pharmacy getPharmacyById(UUID id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", id));
    }

    public Pharmacy updatePharmacy(UUID id, PharmacyRequestDto dto) {
        Pharmacy pharmacy = getPharmacyById(id);

        if (pharmacyRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ConflictException("A pharmacy with email " + dto.getEmail() + " already exists");
        }
        if (pharmacyRepository.existsByLicenseNumberAndIdNot(dto.getLicenseNumber(), id)) {
            throw new ConflictException("A pharmacy with license " + dto.getLicenseNumber() + " already exists");
        }

        mapToEntity(dto, pharmacy);
        return pharmacyRepository.save(pharmacy);
    }

    public void deletePharmacy(UUID id) {
        Pharmacy pharmacy = getPharmacyById(id);
        pharmacyRepository.delete(pharmacy);
    }

    private void mapToEntity(PharmacyRequestDto dto, Pharmacy pharmacy) {
        pharmacy.setName(dto.getName());
        pharmacy.setAddress(dto.getAddress());
        pharmacy.setEmail(dto.getEmail());
        pharmacy.setPhoneNumber(dto.getPhoneNumber());
        pharmacy.setLicenseNumber(dto.getLicenseNumber());
        pharmacy.setKraPin(dto.getKraPin());
    }
}
