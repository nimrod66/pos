package com.example.pos.inventory.batches.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.dto.MedicineBatchRequestDto;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class MedicineBatchesService {

    private final MedicineBatchesRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final AuthenticatedUserContext current;

    public MedicineBatchesService(MedicineBatchesRepository batchRepository,
                                  MedicineRepository medicineRepository,
                                  AuthenticatedUserContext current) {
        this.batchRepository = batchRepository;
        this.medicineRepository = medicineRepository;
        this.current = current;
    }

    public MedicineBatches createBatch(MedicineBatchRequestDto dto) {
        Medicine medicine = scopedMedicine(dto.getMedicineId());
        String batchNumber = normalizeBatch(dto.getBatchNumber());
        if (batchRepository.existsByMedicineIdAndBatchNumberIgnoreCase(medicine.getId(), batchNumber)) {
            throw new ConflictException("Batch number already exists for this medicine",
                    "BATCH_NUMBER_EXISTS");
        }
        if (dto.getInitialQuantity() != 0) {
            throw new BadRequestException("Opening batch quantity must be received through a GRN",
                    "GRN_REQUIRED");
        }
        validateDates(dto, medicine);
        validateSellingPrice(dto, medicine);

        MedicineBatches batch = MedicineBatches.builder()
                .medicine(medicine)
                .batchNumber(batchNumber)
                .manufactureDate(dto.getManufactureDate())
                .expirationDate(dto.getExpirationDate())
                .initialQuantity(0)
                .buyingPrice(money(dto.getBuyingPrice()))
                .sellingPrice(medicine.getSellingPrice())
                .build();
        return batchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getAllBatches(Pageable pageable) {
        return batchRepository.findByMedicinePharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getBatchesByMedicine(UUID medicineId, Pageable pageable) {
        scopedMedicine(medicineId);
        return batchRepository.findByMedicineIdAndMedicinePharmacyId(
                medicineId, current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getAllBatchesByBranch(UUID branchId, Pageable pageable) {
        return batchRepository.findByBranchId(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getBatchesByBranchAndMedicine(UUID branchId, UUID medicineId, Pageable pageable) {
        return batchRepository.findByBranchIdAndMedicineId(branchId, medicineId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getBatchesExpiringBefore(LocalDate date, Pageable pageable) {
        return batchRepository.findByMedicinePharmacyIdAndExpirationDateBefore(
                current.pharmacy().getId(), date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MedicineBatches> getBatchesExpiringBeforeByBranch(UUID branchId, LocalDate date, Pageable pageable) {
        return batchRepository.findByBranchIdAndExpirationDateBefore(branchId, date, pageable);
    }

    @Transactional(readOnly = true)
    public List<MedicineBatches> getAvailableBatchesByMedicine(UUID medicineId) {
        scopedMedicine(medicineId);
        return batchRepository.findByMedicineIdAndMedicinePharmacyIdAndExpirationDateAfter(
                medicineId, current.pharmacy().getId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public MedicineBatches getBatchById(UUID id) {
        return batchRepository.findByIdAndMedicinePharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", id));
    }

    public MedicineBatches updateBatch(UUID id, MedicineBatchRequestDto dto) {
        MedicineBatches batch = getBatchById(id);
        if (!batch.getMedicine().getId().equals(dto.getMedicineId())) {
            throw new BadRequestException("A batch cannot be moved to another medicine");
        }
        if (!batch.getInitialQuantity().equals(dto.getInitialQuantity())) {
            throw new BadRequestException("Batch quantity changes require an audited stock workflow",
                    "DIRECT_STOCK_MUTATION_DISABLED");
        }
        String batchNumber = normalizeBatch(dto.getBatchNumber());
        if (batchRepository.existsByMedicineIdAndBatchNumberIgnoreCaseAndIdNot(
                batch.getMedicine().getId(), batchNumber, id)) {
            throw new ConflictException("Batch number already exists for this medicine",
                    "BATCH_NUMBER_EXISTS");
        }
        validateDates(dto, batch.getMedicine());
        validateSellingPrice(dto, batch.getMedicine());
        batch.setBatchNumber(batchNumber);
        batch.setManufactureDate(dto.getManufactureDate());
        batch.setExpirationDate(dto.getExpirationDate());
        batch.setBuyingPrice(money(dto.getBuyingPrice()));
        batch.setSellingPrice(batch.getMedicine().getSellingPrice());
        return batchRepository.save(batch);
    }

    public void deleteBatch(UUID id) {
        MedicineBatches batch = getBatchById(id);
        if (batch.getStock() != null && !batch.getStock().isEmpty()) {
            throw new ConflictException("A batch with stock history cannot be deleted",
                    "BATCH_IN_USE");
        }
        batchRepository.delete(batch);
    }

    private Medicine scopedMedicine(UUID medicineId) {
        return medicineRepository.findByIdAndPharmacyId(medicineId, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", medicineId));
    }

    private void validateDates(MedicineBatchRequestDto dto, Medicine medicine) {
        if (dto.getManufactureDate() != null && dto.getManufactureDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Manufacture date cannot be in the future");
        }
        if (medicine.isTrackExpiry()
                && (dto.getExpirationDate() == null || !dto.getExpirationDate().isAfter(LocalDate.now()))) {
            throw new BadRequestException("A future expiry date is required", "EXPIRY_DATE_REQUIRED");
        }
        if (dto.getManufactureDate() != null && dto.getExpirationDate() != null
                && !dto.getExpirationDate().isAfter(dto.getManufactureDate())) {
            throw new BadRequestException("Expiry date must be after manufacture date");
        }
    }

    private void validateSellingPrice(MedicineBatchRequestDto dto, Medicine medicine) {
        if (money(dto.getSellingPrice())
                .compareTo(medicine.getSellingPrice()) != 0) {
            throw new ConflictException("Batch selling price must match the medicine selling price",
                    "SELLING_PRICE_MISMATCH");
        }
    }

    private String normalizeBatch(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Money values may have at most two decimal places",
                    "INVALID_MONEY_SCALE");
        }
    }
}
