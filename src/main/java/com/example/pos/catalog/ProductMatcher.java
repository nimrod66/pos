package com.example.pos.catalog;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductMatcher {

    private final MedicineRepository medicineRepository;

    public ProductMatcher(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public MatchResult match(CatalogItem catalogItem) {
        if (catalogItem.getBarcode() != null && !catalogItem.getBarcode().isBlank()) {
            Optional<Medicine> byBarcode = medicineRepository.findByBarcode(catalogItem.getBarcode());
            if (byBarcode.isPresent()) {
                return MatchResult.matched(byBarcode.get(), "HIGH");
            }
        }

        if (catalogItem.getSupplierCode() != null && !catalogItem.getSupplierCode().isBlank()) {
            List<Medicine> all = medicineRepository.findAll();
            for (Medicine med : all) {
                if (catalogItem.getSupplierCode().equals(med.getKemsaCode())
                        || catalogItem.getSupplierCode().equals(med.getPpbCode())
                        || catalogItem.getSupplierCode().equals(med.getManufacturerBarcode())) {
                    return MatchResult.matched(med, "HIGH");
                }
            }
        }

        if (catalogItem.getProductName() != null && !catalogItem.getProductName().isBlank()) {
            List<Medicine> byName = medicineRepository.findByBrandNameContainingIgnoreCase(
                    catalogItem.getProductName().substring(0, Math.min(6, catalogItem.getProductName().length())));
            for (Medicine med : byName) {
                int score = scoreMatch(med, catalogItem);
                if (score >= 70) {
                    return MatchResult.matched(med, score >= 90 ? "HIGH" : "MEDIUM");
                }
            }
        }

        List<Medicine> allMedicines = medicineRepository.findAll();
        Medicine best = null;
        int bestScore = 0;

        for (Medicine med : allMedicines) {
            int score = scoreMatch(med, catalogItem);
            if (score > bestScore) {
                bestScore = score;
                best = med;
            }
        }

        if (bestScore >= 50) {
            return MatchResult.matched(best, "LOW");
        }

        return MatchResult.noMatch("No matching medicine found");
    }

    private int scoreMatch(Medicine medicine, CatalogItem item) {
        int score = 0;

        if (item.getProductName() != null && medicine.getBrandName() != null
                && medicine.getBrandName().toLowerCase().contains(item.getProductName().toLowerCase())) {
            score += 40;
        } else if (item.getProductName() != null && medicine.getBrandName() != null
                && item.getProductName().toLowerCase().contains(medicine.getBrandName().toLowerCase())) {
            score += 30;
        }

        if (item.getGenericName() != null && medicine.getGenericName() != null
                && medicine.getGenericName().equalsIgnoreCase(item.getGenericName())) {
            score += 30;
        } else if (item.getGenericName() != null && medicine.getGenericName() != null
                && medicine.getGenericName().toLowerCase().contains(item.getGenericName().toLowerCase())) {
            score += 20;
        }

        if (item.getStrength() != null && medicine.getStrength() != null
                && medicine.getStrength().equalsIgnoreCase(item.getStrength())) {
            score += 20;
        } else if (item.getStrength() != null && medicine.getStrength() != null
                && medicine.getStrength().toLowerCase().contains(item.getStrength().toLowerCase())) {
            score += 10;
        }

        if (item.getManufacturerName() != null && medicine.getManufacturer() != null
                && medicine.getManufacturer().getManufacturerName().equalsIgnoreCase(item.getManufacturerName())) {
            score += 10;
        }

        return score;
    }

    public record MatchResult(Long medicineId, String medicineName, String confidence, boolean matched, String reason) {
        public static MatchResult matched(Medicine medicine, String confidence) {
            return new MatchResult(medicine.getId(), medicine.getBrandName(), confidence, true, null);
        }

        public static MatchResult noMatch(String reason) {
            return new MatchResult(null, null, "NONE", false, reason);
        }
    }
}
