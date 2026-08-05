package com.example.pos.inventory.stock.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.dto.StockAdjustmentDto;
import com.example.pos.inventory.stock.dto.StockRequestDto;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final MedicineBatchesRepository batchesRepository;
    private final BranchRepository branchRepository;
    private final SyncService syncService;
    private final TerminalConfig terminalConfig;

    public StockService(StockRepository stockRepository,
                        MedicineBatchesRepository batchesRepository,
                        BranchRepository branchRepository,
                        SyncService syncService,
                        TerminalConfig terminalConfig) {
        this.stockRepository = stockRepository;
        this.batchesRepository = batchesRepository;
        this.branchRepository = branchRepository;
        this.syncService = syncService;
        this.terminalConfig = terminalConfig;
    }

    public Stock createStock(StockRequestDto dto) {
        MedicineBatches batch = batchesRepository.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));

        if (stockRepository.findByBranchIdAndMedicineBatchesId(dto.getBranchId(), dto.getMedicineBatchesId()).isPresent()) {
            throw new ConflictException("Stock record already exists for this batch and branch");
        }

        Stock stock = new Stock();
        stock.setMedicineBatches(batch);
        stock.setBranch(branch);
        mapToEntity(dto, stock);
        if (stock.getLastStockDate() == null) {
            stock.setLastStockDate(LocalDate.now());
        }
        return stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public Page<Stock> getStockByBranch(UUID branchId, Pageable pageable) {
        return branchId != null ? stockRepository.findByBranchId(branchId, pageable) : Page.empty();
    }

    @Transactional(readOnly = true)
    public List<Stock> getLowStockByBranch(UUID branchId) {
        List<Stock> all = stockRepository.findByBranchId(branchId);
        return all.stream()
                .filter(s -> s.getQuantityAvailable() != null
                        && s.getReorderLevel() != null
                        && s.getQuantityAvailable() <= s.getReorderLevel())
                .toList();
    }

    @Transactional(readOnly = true)
    public Stock getStockById(UUID id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", id));
    }

    @Transactional(readOnly = true)
    public Stock getStockByBranchAndBatch(UUID branchId, UUID batchId) {
        return stockRepository.findByBranchIdAndMedicineBatchesId(branchId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock record for branch " + branchId + " and batch " + batchId));
    }

    public Stock updateStock(UUID id, StockRequestDto dto) {
        Stock stock = getStockById(id);
        mapToEntity(dto, stock);
        return stockRepository.save(stock);
    }

    public Stock receiveStock(StockAdjustmentDto dto) {
        Stock stock = getStockByBranchAndBatch(dto.getBranchId(), dto.getMedicineBatchesId());
        stock.setQuantityAvailable(
                (stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0) + dto.getQuantity());
        stock.setLastStockDate(LocalDate.now());
        Stock saved = stockRepository.save(stock);

        UUID batchId = stock.getMedicineBatches() != null ? stock.getMedicineBatches().getId() : null;
        String medicineName = stock.getMedicineBatches() != null && stock.getMedicineBatches().getMedicine() != null
                ? stock.getMedicineBatches().getMedicine().getBrandName() : "unknown";
        syncService.writeOutboxEvent(EventType.STOCK_RECEIVED, "STOCK", stock.getId().toString(),
                "{\"batchId\":" + batchId
                        + ",\"medicine\":\"" + medicineName + "\""
                        + ",\"quantity\":" + dto.getQuantity()
                        + ",\"branchId\":" + dto.getBranchId()
                        + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");

        return saved;
    }

    public Stock deductStock(StockAdjustmentDto dto) {
        Stock stock = getStockByBranchAndBatch(dto.getBranchId(), dto.getMedicineBatchesId());
        int current = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0;
        if (current < dto.getQuantity()) {
            throw new ConflictException("Insufficient stock. Available: " + current + ", requested: " + dto.getQuantity());
        }
        stock.setQuantityAvailable(current - dto.getQuantity());
        stock.setLastStockDate(LocalDate.now());
        Stock saved = stockRepository.save(stock);

        UUID batchId = stock.getMedicineBatches() != null ? stock.getMedicineBatches().getId() : null;
        String medicineName = stock.getMedicineBatches() != null && stock.getMedicineBatches().getMedicine() != null
                ? stock.getMedicineBatches().getMedicine().getBrandName() : "unknown";
        syncService.writeOutboxEvent(EventType.STOCK_DEDUCTED, "STOCK", stock.getId().toString(),
                "{\"batchId\":" + batchId
                        + ",\"medicine\":\"" + medicineName + "\""
                        + ",\"quantity\":" + dto.getQuantity()
                        + ",\"branchId\":" + dto.getBranchId()
                        + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");

        return saved;
    }

    public void deleteStock(UUID id) {
        Stock stock = getStockById(id);
        stockRepository.delete(stock);
    }

    private void mapToEntity(StockRequestDto dto, Stock stock) {
        stock.setQuantityAvailable(dto.getQuantityAvailable());
        stock.setReservedQuantity(dto.getReservedQuantity());
        stock.setMinimumStock(dto.getMinimumStock());
        stock.setMaximumStock(dto.getMaximumStock());
        stock.setReorderLevel(dto.getReorderLevel());
        stock.setShelfLocation(dto.getShelfLocation());
        stock.setLastStockDate(dto.getLastStockDate());
    }
}
