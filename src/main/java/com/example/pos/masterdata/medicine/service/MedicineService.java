package com.example.pos.masterdata.medicine.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.categories.repository.MedicineCategoriesRepository;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.dosage.repository.DosageFormRepository;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.manufacturer.repository.ManufacturerRepository;
import com.example.pos.masterdata.medicine.dto.MedicineRequestDto;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.repository.TaxRepository;
import com.example.pos.masterdata.units.model.Unit;
import com.example.pos.masterdata.units.repository.UnitRepository;
import com.example.pos.terminal.barcode.BarcodeSource;
import com.example.pos.terminal.barcode.BarcodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final MedicineCategoriesRepository categoriesRepository;
    private final DosageFormRepository dosageFormRepository;
    private final UnitRepository unitRepository;
    private final TaxRepository taxRepository;

    public MedicineService(MedicineRepository medicineRepository,
                           ManufacturerRepository manufacturerRepository,
                           MedicineCategoriesRepository categoriesRepository,
                           DosageFormRepository dosageFormRepository,
                           UnitRepository unitRepository,
                           TaxRepository taxRepository) {
        this.medicineRepository = medicineRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.categoriesRepository = categoriesRepository;
        this.dosageFormRepository = dosageFormRepository;
        this.unitRepository = unitRepository;
        this.taxRepository = taxRepository;
    }

    @Auditable(action = "CREATE_MEDICINE", entity = "Medicine")
    public Medicine createMedicine(MedicineRequestDto dto) {
        if (medicineRepository.existsByBarcode(dto.getBarcode())) {
            throw new ConflictException("Barcode " + dto.getBarcode() + " already exists");
        }
        if (dto.getSku() != null && medicineRepository.existsBySku(dto.getSku())) {
            throw new ConflictException("SKU " + dto.getSku() + " already exists");
        }

        Medicine medicine = new Medicine();
        resolveReferences(dto, medicine);
        mapToEntity(dto, medicine);
        medicine.setStatus(Medicine.Status.AVAILABLE);
        return medicineRepository.save(medicine);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getAllMedicines(Pageable pageable) {
        return medicineRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getMedicinesByCategory(UUID categoryId, Pageable pageable) {
        return medicineRepository.findByMedicineCategoriesId(categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getMedicinesByManufacturer(UUID manufacturerId, Pageable pageable) {
        return medicineRepository.findByManufacturerId(manufacturerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> getControlledDrugs(Pageable pageable) {
        return medicineRepository.findByIsControlledDrugTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Medicine> search(String q, Pageable pageable) {
        return medicineRepository.search(q, pageable);
    }

    @Transactional(readOnly = true)
    public Medicine getMedicineById(UUID id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
    }

    @Transactional(readOnly = true)
    public Medicine getMedicineByBarcode(String barcode) {
        return medicineRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine with barcode " + barcode));
    }

    @Auditable(action = "UPDATE_MEDICINE", entity = "Medicine")
    public Medicine updateMedicine(UUID id, MedicineRequestDto dto) {
        Medicine medicine = getMedicineById(id);

        if (medicineRepository.existsByBarcodeAndIdNot(dto.getBarcode(), id)) {
            throw new ConflictException("Barcode " + dto.getBarcode() + " already exists");
        }
        if (dto.getSku() != null && medicineRepository.existsBySkuAndIdNot(dto.getSku(), id)) {
            throw new ConflictException("SKU " + dto.getSku() + " already exists");
        }

        resolveReferences(dto, medicine);
        mapToEntity(dto, medicine);
        return medicineRepository.save(medicine);
    }

    @Auditable(action = "DELETE_MEDICINE", entity = "Medicine")
    public void deleteMedicine(UUID id) {
        Medicine medicine = getMedicineById(id);
        medicineRepository.delete(medicine);
    }

    private void resolveReferences(MedicineRequestDto dto, Medicine medicine) {
        Manufacturer manufacturer = manufacturerRepository.findById(dto.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", dto.getManufacturerId()));
        medicine.setManufacturer(manufacturer);

        MedicineCategories category = categoriesRepository.findById(dto.getMedicineCategoriesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineCategories", dto.getMedicineCategoriesId()));
        medicine.setMedicineCategories(category);

        if (dto.getDosageFormId() != null) {
            DosageForm form = dosageFormRepository.findById(dto.getDosageFormId())
                    .orElseThrow(() -> new ResourceNotFoundException("DosageForm", dto.getDosageFormId()));
            medicine.setDosageForm(form);
        }

        if (dto.getUnitId() != null) {
            Unit unit = unitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", dto.getUnitId()));
            medicine.setUnit(unit);
        }

        if (dto.getTaxId() != null) {
            Tax tax = taxRepository.findById(dto.getTaxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax", dto.getTaxId()));
            medicine.setTax(tax);
        }
    }

    private void mapToEntity(MedicineRequestDto dto, Medicine medicine) {
        medicine.setBarcode(dto.getBarcode());
        medicine.setManufacturerBarcode(dto.getManufacturerBarcode());
        medicine.setInternalBarcode(dto.getInternalBarcode());
        medicine.setKemsaCode(dto.getKemsaCode());
        medicine.setPpbCode(dto.getPpbCode());
        medicine.setEtimsItemCode(dto.getEtimsItemCode());
        medicine.setGs1CompanyPrefix(dto.getGs1CompanyPrefix());
        if (dto.getBarcodeType() != null) {
            try {
                medicine.setBarcodeType(BarcodeType.valueOf(dto.getBarcodeType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid barcode type: " + dto.getBarcodeType());
            }
        }
        if (dto.getBarcodeSource() != null) {
            try {
                medicine.setBarcodeSource(BarcodeSource.valueOf(dto.getBarcodeSource().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid barcode source: " + dto.getBarcodeSource());
            }
        }
        medicine.setTrackSerialNumber(dto.isTrackSerialNumber());
        medicine.setTrackBatch(dto.isTrackBatch());
        medicine.setTrackExpiry(dto.isTrackExpiry());
        medicine.setSku(dto.getSku());
        medicine.setBrandName(dto.getBrandName());
        medicine.setGenericName(dto.getGenericName());
        medicine.setStrength(dto.getStrength());
        medicine.setRequiresPrescription(dto.isRequiresPrescription());
        medicine.setDescription(dto.getDescription());
        medicine.setMaximumDispenseQuantity(dto.getMaximumDispenseQuantity());
        medicine.setMinimumAge(dto.getMinimumAge());
        medicine.setRequiresRefrigeration(dto.isRequiresRefrigeration());
        medicine.setControlledDrug(dto.isControlledDrug());
        if (dto.getStatus() != null) {
            try {
                medicine.setStatus(Medicine.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + dto.getStatus());
            }
        }
    }
}
