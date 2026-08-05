package com.example.pos.masterdata.dosage.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.dosage.dto.DosageFormRequestDto;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.dosage.repository.DosageFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DosageFormService {

    private final DosageFormRepository repository;

    public DosageFormService(DosageFormRepository repository) {
        this.repository = repository;
    }

    public DosageForm create(DosageFormRequestDto dto) {
        if (repository.existsByFormName(dto.getFormName())) {
            throw new ConflictException("Dosage form '" + dto.getFormName() + "' already exists");
        }
        DosageForm form = new DosageForm();
        form.setFormName(dto.getFormName());
        form.setFormDescription(dto.getFormDescription());
        return repository.save(form);
    }

    @Transactional(readOnly = true)
    public List<DosageForm> getAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public DosageForm getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("DosageForm", id));
    }

    public DosageForm update(UUID id, DosageFormRequestDto dto) {
        DosageForm form = getById(id);
        if (repository.existsByFormNameAndIdNot(dto.getFormName(), id)) {
            throw new ConflictException("Dosage form '" + dto.getFormName() + "' already exists");
        }
        form.setFormName(dto.getFormName());
        form.setFormDescription(dto.getFormDescription());
        return repository.save(form);
    }

    public void delete(UUID id) { repository.delete(getById(id)); }
}
