package com.example.pos.inventory.batches.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.dto.MedicineBatchRequestDto;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MedicineBatchesService {

    private final MedicineBatchesRepository batchRepository;
    private final MedicineRepository medicineRepository;

    public MedicineBatchesService(MedicineBatchesRepository batchRepository,
                                  MedicineRepository medicineRepository) {
        this.batchRepository = batchRepository;
        this.medicineRepository = medicineRepository;
    }

    public MedicineBatches createBatch(MedicineBatchRequestDto dto) {
        Medicine medicine = medicineRepository.findById(dto.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", dto.getMedicineId()));

        if (batchRepository.existsByBatchNumber(dto.getBatchNumber())) {
            throw new ConflictException("Batch number " + dto.getBatchNumber() + " already exists");
        }

        MedicineBatches batch = new MedicineBatches();
        batch.setMedicine(medicine);
        mapToEntity(dto, batch);
        return batchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<MedicineBatches> getAllBatches() {
        return batchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MedicineBatches> getBatchesByMedicine(Long medicineId) {
        return batchRepository.findByMedicineId(medicineId);
    }

    @Transactional(readOnly = true)
    public List<MedicineBatches> getBatchesExpiringBefore(LocalDate date) {
        return batchRepository.findByExpirationDateBefore(date);
    }

    @Transactional(readOnly = true)
    public List<MedicineBatches> getAvailableBatchesByMedicine(Long medicineId) {
        return batchRepository.findByMedicineIdAndExpirationDateAfter(medicineId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public MedicineBatches getBatchById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", id));
    }

    public MedicineBatches updateBatch(Long id, MedicineBatchRequestDto dto) {
        MedicineBatches batch = getBatchById(id);

        if (!batch.getMedicine().getId().equals(dto.getMedicineId())) {
            Medicine medicine = medicineRepository.findById(dto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", dto.getMedicineId()));
            batch.setMedicine(medicine);
        }

        mapToEntity(dto, batch);
        return batchRepository.save(batch);
    }

    public void deleteBatch(Long id) {
        MedicineBatches batch = getBatchById(id);
        batchRepository.delete(batch);
    }

    private void mapToEntity(MedicineBatchRequestDto dto, MedicineBatches batch) {
        batch.setBatchNumber(dto.getBatchNumber());
        batch.setManufactureDate(dto.getManufactureDate());
        batch.setExpirationDate(dto.getExpirationDate());
        batch.setInitialQuantity(dto.getInitialQuantity());
        batch.setBuyingPrice(dto.getBuyingPrice());
        batch.setSellingPrice(dto.getSellingPrice());
    }
}
