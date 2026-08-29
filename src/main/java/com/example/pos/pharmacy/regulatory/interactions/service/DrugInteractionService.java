package com.example.pos.pharmacy.regulatory.interactions.service;

import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.pharmacy.regulatory.interactions.dto.DrugInteractionResponse;
import com.example.pos.pharmacy.regulatory.interactions.model.DrugInteraction;
import com.example.pos.pharmacy.regulatory.interactions.repository.DrugInteractionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrugInteractionService {

    private final DrugInteractionRepository repository;
    private final EntityManager entityManager;

    public List<DrugInteractionResponse> checkInteractions(List<UUID> medicineIds) {
        if (medicineIds == null || medicineIds.size() < 2) {
            return Collections.emptyList();
        }
        List<DrugInteraction> interactions = repository.findAllForMedicines(medicineIds);
        return interactions.stream()
                .map(DrugInteractionResponse::from)
                .collect(Collectors.toList());
    }

    public List<DrugInteractionResponse> checkPair(UUID medicine1Id, UUID medicine2Id) {
        return repository.findBetween(medicine1Id, medicine2Id).stream()
                .map(DrugInteractionResponse::from)
                .collect(Collectors.toList());
    }

    public List<DrugInteractionResponse> findAll() {
        return repository.findAll().stream()
                .map(DrugInteractionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public DrugInteractionResponse create(UUID medicine1Id, UUID medicine2Id,
                                          DrugInteraction.Severity severity, String description) {
        Medicine m1 = entityManager.getReference(Medicine.class, medicine1Id);
        Medicine m2 = entityManager.getReference(Medicine.class, medicine2Id);
        DrugInteraction interaction = DrugInteraction.builder()
                .medicine1(m1)
                .medicine2(m2)
                .severity(severity)
                .description(description)
                .build();
        return DrugInteractionResponse.from(repository.save(interaction));
    }

    @Transactional
    public void deactivate(UUID id) {
        repository.findById(id).ifPresent(di -> {
            di.setActive(false);
            repository.save(di);
        });
    }
}
