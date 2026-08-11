package com.example.pos.core.pharmacy.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.pharmacy.dto.PharmacyRequestDto;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.repository.PharmacyRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final AuthenticatedUserContext current;

    public PharmacyService(PharmacyRepository pharmacyRepository,
                           AuthenticatedUserContext current) {
        this.pharmacyRepository = pharmacyRepository;
        this.current = current;
    }

    public Pharmacy createPharmacy(PharmacyRequestDto dto) {
        throw new ForbiddenException("Pharmacy creation is handled by the installation/onboarding process");
    }

    @Transactional(readOnly = true)
    public List<Pharmacy> getAllPharmacies() {
        return List.of(current.pharmacy());
    }

    @Transactional(readOnly = true)
    public Pharmacy getPharmacyById(UUID id) {
        current.requirePharmacy(id);
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", id));
    }

    public Pharmacy updatePharmacy(UUID id, PharmacyRequestDto dto) {
        Pharmacy pharmacy = getPharmacyById(id);
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (pharmacyRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("A pharmacy with email " + email + " already exists");
        }
        if (pharmacyRepository.existsByLicenseNumberAndIdNot(dto.getLicenseNumber().trim(), id)) {
            throw new ConflictException("A pharmacy with license " + dto.getLicenseNumber() + " already exists");
        }
        pharmacy.setName(dto.getName().trim());
        pharmacy.setAddress(dto.getAddress().trim());
        pharmacy.setEmail(email);
        pharmacy.setPhoneNumber(dto.getPhoneNumber().trim());
        pharmacy.setLicenseNumber(dto.getLicenseNumber().trim());
        pharmacy.setKraPin(dto.getKraPin().trim().toUpperCase(Locale.ROOT));
        return pharmacyRepository.save(pharmacy);
    }

    public void deletePharmacy(UUID id) {
        current.requirePharmacy(id);
        throw new ForbiddenException("Pharmacy deletion is not available from the POS");
    }
}
