package com.example.pos.pharmacy.regulatory.expiry.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.pharmacy.regulatory.expiry.dto.ExpiryLogRequestDto;
import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import com.example.pos.pharmacy.regulatory.expiry.repository.ExpiryLogsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExpiryLogsService {

    private final ExpiryLogsRepository repo;
    private final MedicineBatchesRepository batchRepo;
    private final StockRepository stockRepository;
    private final StockMovementsRepository movementsRepository;
    private final com.example.pos.security.auth.AuthenticatedUserContext current;

    public ExpiryLogsService(ExpiryLogsRepository repo,
                             MedicineBatchesRepository batchRepo,
                             StockRepository stockRepository,
                             StockMovementsRepository movementsRepository,
                             com.example.pos.security.auth.AuthenticatedUserContext current) {
        this.repo = repo;
        this.batchRepo = batchRepo;
        this.stockRepository = stockRepository;
        this.movementsRepository = movementsRepository;
        this.current = current;
    }

    /**
     * Writes off expired stock from the signed-in user's branch: records the
     * regulatory disposal log, decrements sellable quantity under a row lock,
     * and journals an EXPIRED movement.
     */
    public ExpiryLogs log(ExpiryLogRequestDto dto) {
        MedicineBatches batch = batchRepo.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch",
                        dto.getMedicineBatchesId()));

        Branch branch = currentBranch();
        Stock stock = stockRepository.findForUpdate(branch.getId(), batch.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock for batch " + batch.getId()));

        int available = stock.getQuantityAvailable() == null ? 0 : stock.getQuantityAvailable();
        if (dto.getQuantityDisposed() > available) {
            throw new ConflictException("Only " + available
                    + " unit(s) of this batch remain at " + branch.getBranchName(),
                    "INSUFFICIENT_STOCK");
        }
        stock.setQuantityAvailable(available - dto.getQuantityDisposed());
        stock.setLastStockDate(java.time.LocalDate.now());

        ExpiryLogs log = new ExpiryLogs();
        log.setMedicineBatches(batch);
        log.setUser(currentUser());
        log.setDisposalMethod(dto.getDisposalMethod()
                + " (" + dto.getQuantityDisposed() + " unit(s))");
        ExpiryLogs saved = repo.save(log);

        movementsRepository.save(StockMovements.builder()
                .medicineBatches(batch)
                .branch(branch)
                .user(currentUser())
                .movementType(StockMovements.MovementType.EXPIRED)
                .referenceType("EXPIRY_LOG")
                .referenceId(saved.getId())
                .movementDate(java.time.LocalDate.now())
                .quantity(dto.getQuantityDisposed())
                .build());
        return saved;
    }

    private Branch currentBranch() {
        return currentUser().getBranch();
    }

    private com.example.pos.user.users.model.User currentUser() {
        return current.user();
    }

    @Transactional(readOnly = true)
    public List<ExpiryLogs> getByBatch(UUID batchId) { return repo.findByMedicineBatchesId(batchId); }

    @Transactional(readOnly = true)
    public List<ExpiryLogs> getAll() { return repo.findAll(); }
}
