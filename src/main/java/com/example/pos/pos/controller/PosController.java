package com.example.pos.pos.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pos")
public class PosController {

    private final MedicineRepository medicineRepo;
    private final StockRepository stockRepo;
    private final AuthenticatedUserContext current;
    private final Clock clock;

    public PosController(MedicineRepository medicineRepo, StockRepository stockRepo,
                         AuthenticatedUserContext current, Clock clock) {
        this.medicineRepo = medicineRepo;
        this.stockRepo = stockRepo;
        this.current = current;
        this.clock = clock;
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('pos.sell')")
    public ApiResponse<List<Map<String, Object>>> lookup(
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID branchId) {
        if (branchId != null) current.requireBranch(branchId);
        UUID activeBranchId = current.branchId();
        String query = barcode != null && !barcode.isBlank() ? barcode.trim()
                : name != null && !name.isBlank() ? name.trim() : null;
        List<Medicine> medicines = query == null ? List.of()
                : medicineRepo.searchForPos(current.pharmacyId(), query, PageRequest.of(0, 30));

        List<Map<String, Object>> result = medicines.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("sku", m.getSku());
            map.put("barcode", m.getBarcode());
            map.put("brandName", m.getBrandName());
            map.put("genericName", m.getGenericName());
            map.put("strength", m.getStrength());
            map.put("categoryId", m.getMedicineCategories() == null
                    ? null : m.getMedicineCategories().getId());
            map.put("requiresPrescription", m.isRequiresPrescription());
            map.put("isControlledDrug", m.isControlledDrug());

            List<Stock> stocks = sellableStocks(activeBranchId, m.getId());
            long available = stocks.stream()
                    .mapToInt(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0)
                    .sum();
            map.put("stockAvailable", available);
            map.put("sellingPrice", m.getSellingPrice());

            var batchPrices = stocks.stream()
                    .map(s -> {
                        Map<String, Object> batch = new HashMap<>();
                        batch.put("batchId", s.getMedicineBatches().getId());
                        batch.put("batchNumber", s.getMedicineBatches().getBatchNumber());
                        batch.put("available", s.getQuantityAvailable());
                        batch.put("sellingPrice", m.getSellingPrice());
                        batch.put("expirationDate", s.getMedicineBatches().getExpirationDate());
                        return batch;
                    })
                    .collect(Collectors.toList());
            map.put("batches", batchPrices);
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/quick-items")
    @PreAuthorize("hasAuthority('pos.sell')")
    public ApiResponse<List<Map<String, Object>>> quickItems(
            @RequestParam(required = false) UUID branchId) {
        if (branchId != null) current.requireBranch(branchId);
        List<Stock> stocks = stockRepo.findByBranchIdAndQuantityAvailableGreaterThan(
                        current.branchId(), 0).stream()
                .filter(this::isSellable)
                .toList();
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
                    map.put("sellingPrice", m.getSellingPrice());
                    map.put("expirationDate", s.getMedicineBatches().getExpirationDate());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    private List<Stock> sellableStocks(UUID branchId, UUID medicineId) {
        return stockRepo.findByBranchIdAndMedicineBatches_Medicine_Id(branchId, medicineId)
                .stream()
                .filter(this::isSellable)
                .toList();
    }

    private boolean isSellable(Stock stock) {
        if (stock.getQuantityAvailable() == null || stock.getQuantityAvailable() <= 0
                || stock.getMedicineBatches() == null
                || stock.getMedicineBatches().getMedicine() == null
                || stock.getMedicineBatches().getMedicine().getStatus() != Medicine.Status.AVAILABLE) {
            return false;
        }
        LocalDate expiry = stock.getMedicineBatches().getExpirationDate();
        return expiry == null || expiry.isAfter(LocalDate.now(clock));
    }
}
