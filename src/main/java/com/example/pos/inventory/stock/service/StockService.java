package com.example.pos.inventory.stock.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.dto.StockAdjustmentDto;
import com.example.pos.inventory.stock.dto.StockRequestDto;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final MedicineBatchesRepository batchesRepository;
    private final AuthenticatedUserContext current;

    public StockService(StockRepository stockRepository,
                        MedicineBatchesRepository batchesRepository,
                        AuthenticatedUserContext current) {
        this.stockRepository = stockRepository;
        this.batchesRepository = batchesRepository;
        this.current = current;
    }

    public Stock createStock(StockRequestDto dto) {
        Branch branch = current.branch();
        current.requireBranch(dto.getBranchId());
        MedicineBatches batch = batchesRepository.findByIdAndMedicinePharmacyId(
                        dto.getMedicineBatchesId(), branch.getPharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        if (stockRepository.findByBranchIdAndMedicineBatchesId(branch.getId(), batch.getId()).isPresent()) {
            throw new ConflictException("Stock record already exists for this batch and branch");
        }
        if (value(dto.getQuantityAvailable()) != 0 || value(dto.getReservedQuantity()) != 0) {
            throw new BadRequestException("Opening stock must be received through a GRN",
                    "GRN_REQUIRED");
        }

        Stock stock = Stock.builder()
                .medicineBatches(batch)
                .branch(branch)
                .quantityAvailable(0)
                .reservedQuantity(0)
                .minimumStock(value(dto.getMinimumStock()))
                .maximumStock(dto.getMaximumStock())
                .reorderLevel(dto.getReorderLevel() != null
                        ? dto.getReorderLevel() : batch.getMedicine().getReorderLevel())
                .shelfLocation(trimToNull(dto.getShelfLocation()))
                .lastStockDate(dto.getLastStockDate() != null ? dto.getLastStockDate() : LocalDate.now())
                .build();
        return stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public Page<Stock> getStockByBranch(UUID branchId, Pageable pageable) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return stockRepository.findByBranchId(branch.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Stock> getLowStockByBranch(UUID branchId) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return stockRepository.findByBranchId(branch.getId()).stream()
                .filter(stock -> stock.getQuantityAvailable() != null
                        && stock.getReorderLevel() != null
                        && stock.getQuantityAvailable() <= stock.getReorderLevel())
                .toList();
    }

    @Transactional(readOnly = true)
    public Stock getStockById(UUID id) {
        return stockRepository.findDetailedByIdAndBranchId(id, current.branch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", id));
    }

    @Transactional(readOnly = true)
    public Stock getStockByBranchAndBatch(UUID branchId, UUID batchId) {
        current.requireBranch(branchId);
        return stockRepository.findByBranchIdAndMedicineBatchesId(branchId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock record for branch " + branchId + " and batch " + batchId));
    }

    public Stock updateStock(UUID id, StockRequestDto dto) {
        Stock stock = getStockById(id);
        current.requireBranch(dto.getBranchId());
        if (!stock.getMedicineBatches().getId().equals(dto.getMedicineBatchesId())) {
            throw new BadRequestException("A stock record cannot be moved to another batch");
        }
        if (dto.getQuantityAvailable() != null
                && !Objects.equals(dto.getQuantityAvailable(), stock.getQuantityAvailable())) {
            throw new BadRequestException("Quantity changes require an audited stock workflow",
                    "DIRECT_STOCK_MUTATION_DISABLED");
        }
        if (dto.getReservedQuantity() != null
                && !Objects.equals(dto.getReservedQuantity(), stock.getReservedQuantity())) {
            throw new BadRequestException("Reserved quantity cannot be edited directly",
                    "DIRECT_STOCK_MUTATION_DISABLED");
        }
        stock.setMinimumStock(dto.getMinimumStock());
        stock.setMaximumStock(dto.getMaximumStock());
        stock.setReorderLevel(dto.getReorderLevel());
        stock.setShelfLocation(trimToNull(dto.getShelfLocation()));
        return stockRepository.save(stock);
    }

    public Stock receiveStock(StockAdjustmentDto dto) {
        current.requireBranch(dto.getBranchId());
        throw new BadRequestException("Receive stock through a goods-received note",
                "GRN_REQUIRED");
    }

    public Stock deductStock(StockAdjustmentDto dto) {
        current.requireBranch(dto.getBranchId());
        throw new BadRequestException("Use checkout, returns, or an approved adjustment workflow",
                "DIRECT_STOCK_MUTATION_DISABLED");
    }

    public void deleteStock(UUID id) {
        Stock stock = getStockById(id);
        if (value(stock.getQuantityAvailable()) != 0 || value(stock.getReservedQuantity()) != 0) {
            throw new ConflictException("Stock with quantity or reservations cannot be deleted",
                    "STOCK_NOT_EMPTY");
        }
        stockRepository.delete(stock);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
