package com.example.pos.pos.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final MedicineRepository medicineRepo;
    private final StockRepository stockRepo;

    public PosController(MedicineRepository medicineRepo, StockRepository stockRepo) {
        this.medicineRepo = medicineRepo;
        this.stockRepo = stockRepo;
    }

    @GetMapping("/lookup")
    public ApiResponse<List<Map<String, Object>>> lookup(
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long branchId) {
        List<Medicine> medicines;

        if (barcode != null && !barcode.isBlank()) {
            medicines = medicineRepo.findByBarcode(barcode)
                    .map(List::of)
                    .orElseGet(() -> {
                        if (barcode.length() >= 4) {
                            List<Medicine> ending = medicineRepo.findByBarcodeEndingWithOrderByBarcodeAsc(barcode);
                            if (!ending.isEmpty()) return ending;
                            List<Medicine> containing = medicineRepo.findByBarcodeContaining(barcode);
                            if (!containing.isEmpty()) return containing;
                        }
                        return List.of();
                    });
        } else if (name != null && !name.isBlank()) {
            medicines = medicineRepo.findByBrandNameContainingIgnoreCase(name);
        } else {
            medicines = List.of();
        }

        List<Map<String, Object>> result = medicines.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("barcode", m.getBarcode());
            map.put("brandName", m.getBrandName());
            map.put("genericName", m.getGenericName());
            map.put("strength", m.getStrength());
            map.put("requiresPrescription", m.isRequiresPrescription());
            map.put("isControlledDrug", m.isControlledDrug());

            if (branchId != null) {
                List<Stock> stocks = stockRepo.findByBranchIdAndMedicineBatches_Medicine_Id(branchId, m.getId());
                long available = stocks.stream()
                        .mapToInt(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0)
                        .sum();
                map.put("stockAvailable", available);

                var batchPrices = stocks.stream()
                        .filter(s -> s.getQuantityAvailable() != null && s.getQuantityAvailable() > 0)
                        .map(s -> {
                            Map<String, Object> batch = new HashMap<>();
                            batch.put("batchId", s.getMedicineBatches().getId());
                            batch.put("batchNumber", s.getMedicineBatches().getBatchNumber());
                            batch.put("available", s.getQuantityAvailable());
                            batch.put("sellingPrice", s.getMedicineBatches().getSellingPrice());
                            batch.put("expirationDate", s.getMedicineBatches().getExpirationDate());
                            return batch;
                        })
                        .collect(Collectors.toList());
                map.put("batches", batchPrices);
            }
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/quick-items")
    public ApiResponse<List<Map<String, Object>>> quickItems(@RequestParam Long branchId) {
        List<Stock> stocks = stockRepo.findByBranchIdAndQuantityAvailableGreaterThan(branchId, 0);
        List<Map<String, Object>> result = stocks.stream()
                .filter(s -> s.getMedicineBatches() != null && s.getMedicineBatches().getMedicine() != null)
                .map(s -> {
                    Medicine m = s.getMedicineBatches().getMedicine();
                    Map<String, Object> map = new HashMap<>();
                    map.put("batchId", s.getMedicineBatches().getId());
                    map.put("batchNumber", s.getMedicineBatches().getBatchNumber());
                    map.put("medicineId", m.getId());
                    map.put("barcode", m.getBarcode());
                    map.put("name", m.getBrandName());
                    map.put("strength", m.getStrength());
                    map.put("available", s.getQuantityAvailable());
                    map.put("sellingPrice", s.getMedicineBatches().getSellingPrice());
                    map.put("expirationDate", s.getMedicineBatches().getExpirationDate());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }
}
