package com.example.pos.inventory.stockmovements.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stockmovements.dto.StockMovementRequestDto;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class StockMovementsService {

    private final StockMovementsRepository movementsRepository;
    private final MedicineBatchesRepository batchesRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SyncService syncService;
    private final TerminalConfig terminalConfig;

    public StockMovementsService(StockMovementsRepository movementsRepository,
                                 MedicineBatchesRepository batchesRepository,
                                 BranchRepository branchRepository,
                                 UserRepository userRepository,
                                 SyncService syncService,
                                 TerminalConfig terminalConfig) {
        this.movementsRepository = movementsRepository;
        this.batchesRepository = batchesRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.syncService = syncService;
        this.terminalConfig = terminalConfig;
    }

    public StockMovements recordMovement(StockMovementRequestDto dto) {
        MedicineBatches batch = batchesRepository.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        StockMovements movement = new StockMovements();
        movement.setMedicineBatches(batch);
        movement.setBranch(branch);
        movement.setUser(user);
        try {
            movement.setMovementType(StockMovements.MovementType.valueOf(dto.getMovementType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid movement type: " + dto.getMovementType());
        }
        movement.setReferenceType(dto.getReferenceType());
        movement.setReferenceId(dto.getReferenceId());
        movement.setMovementDate(dto.getMovementDate() != null ? dto.getMovementDate() : LocalDate.now());

        StockMovements saved = movementsRepository.save(movement);

        syncService.writeOutboxEvent(EventType.STOCK_MOVEMENT, "STOCK",
                saved.getId().toString(),
                "{\"type\":\"" + dto.getMovementType() + "\""
                        + ",\"batchId\":" + dto.getMedicineBatchesId()
                        + ",\"branchId\":" + dto.getBranchId()
                        + ",\"referenceType\":\"" + dto.getReferenceType() + "\""
                        + ",\"referenceId\":" + dto.getReferenceId()
                        + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");

        return saved;
    }

    public StockMovements recordDirect(UUID branchId, UUID batchId, UUID userId,
                                        String movementType, Integer quantity,
                                        String referenceType, UUID referenceId) {
        MedicineBatches batch = batchesRepository.findById(batchId).orElse(null);
        Branch branch = branchRepository.findById(branchId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        StockMovements movement = StockMovements.builder()
                .medicineBatches(batch)
                .branch(branch)
                .user(user)
                .movementType(StockMovements.MovementType.valueOf(movementType))
                .referenceType(referenceType)
                .referenceId(referenceId)
                .quantity(quantity)
                .movementDate(LocalDate.now())
                .build();
        StockMovements saved = movementsRepository.save(movement);

        syncService.writeOutboxEvent(EventType.STOCK_MOVEMENT, "STOCK",
                saved.getId().toString(),
                "{\"type\":\"" + movementType + "\""
                        + ",\"batchId\":" + batchId
                        + ",\"branchId\":" + branchId
                        + ",\"quantity\":" + quantity
                        + ",\"referenceType\":\"" + referenceType + "\""
                        + ",\"referenceId\":" + referenceId
                        + ",\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<StockMovements> getMovementsByBatch(UUID batchId, Pageable pageable) {
        return movementsRepository.findByMedicineBatchesId(batchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovements> getMovementsByBranch(UUID branchId, Pageable pageable) {
        return movementsRepository.findByBranchId(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovements> getMovementsByBranchAndDateRange(
            UUID branchId, LocalDate start, LocalDate end, Pageable pageable) {
        return movementsRepository.findByBranchIdAndMovementDateBetween(branchId, start, end, pageable);
    }

    @Transactional(readOnly = true)
    public StockMovements getMovementById(UUID id) {
        return movementsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockMovement", id));
    }
}
