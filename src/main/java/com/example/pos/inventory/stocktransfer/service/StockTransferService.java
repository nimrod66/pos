package com.example.pos.inventory.stocktransfer.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.inventory.stocktransfer.dto.StockTransferRequestDto;
import com.example.pos.inventory.stocktransfer.dto.StockTransferResponseDto;
import com.example.pos.inventory.stocktransfer.model.StockTransfer;
import com.example.pos.inventory.stocktransfer.model.StockTransferItem;
import com.example.pos.inventory.stocktransfer.repository.StockTransferRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StockTransferService {

    private final StockTransferRepository repo;
    private final BranchRepository branchRepo;
    private final MedicineBatchesRepository batchRepo;
    private final StockRepository stockRepo;
    private final StockMovementsRepository movementsRepo;
    private final AuthenticatedUserContext current;

    public StockTransferService(StockTransferRepository repo, BranchRepository branchRepo,
                                MedicineBatchesRepository batchRepo, StockRepository stockRepo,
                                StockMovementsRepository movementsRepo,
                                AuthenticatedUserContext current) {
        this.repo = repo;
        this.branchRepo = branchRepo;
        this.batchRepo = batchRepo;
        this.stockRepo = stockRepo;
        this.movementsRepo = movementsRepo;
        this.current = current;
    }

    public StockTransferResponseDto createTransfer(StockTransferRequestDto dto) {
        Branch sourceBranch = branchRepo.findById(dto.getSourceBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Source branch", dto.getSourceBranchId()));
        Branch destBranch = branchRepo.findById(dto.getDestBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination branch", dto.getDestBranchId()));
        if (dto.getSourceBranchId().equals(dto.getDestBranchId())) {
            throw new BadRequestException("Source and destination branches must be different");
        }

        User requestedBy = current.user();
        StockTransfer st = StockTransfer.builder()
                .sourceBranch(sourceBranch)
                .destBranch(destBranch)
                .requestedBy(requestedBy)
                .status(StockTransfer.Status.PENDING.name())
                .remarks(dto.getRemarks())
                .build();
        st = repo.save(st);

        if (dto.getItems() != null) {
            for (StockTransferRequestDto.StockTransferItemDto itemDto : dto.getItems()) {
                MedicineBatches batch = batchRepo.findById(itemDto.getMedicineBatchesId())
                        .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", itemDto.getMedicineBatchesId()));

                Stock sourceStock = stockRepo.findByBranchIdAndMedicineBatchesId(sourceBranch.getId(), batch.getId())
                        .orElse(null);
                int available = sourceStock != null && sourceStock.getQuantityAvailable() != null
                        ? sourceStock.getQuantityAvailable() : 0;
                if (available < itemDto.getQuantity()) {
                    throw new ConflictException("Insufficient stock for " + batch.getBatchNumber()
                            + ". Available: " + available + ", requested: " + itemDto.getQuantity());
                }

                StockTransferItem item = StockTransferItem.builder()
                        .stockTransfer(st)
                        .medicineBatches(batch)
                        .quantity(itemDto.getQuantity())
                        .receivedQuantity(0)
                        .build();
                st.getItems().add(item);
            }
        }

        st = repo.save(st);
        return StockTransferResponseDto.from(st);
    }

    public StockTransferResponseDto approveTransfer(UUID transferId) {
        StockTransfer st = repo.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", transferId));
        if (!StockTransfer.Status.PENDING.name().equals(st.getStatus())) {
            throw new BadRequestException("Only PENDING transfers can be approved");
        }

        User approvedBy = current.user();
        Branch sourceBranch = st.getSourceBranch();
        LocalDate today = LocalDate.now();

        for (StockTransferItem item : st.getItems()) {
            Stock sourceStock = stockRepo.findByBranchIdAndMedicineBatchesId(sourceBranch.getId(), item.getMedicineBatches().getId())
                    .orElse(null);
            int available = sourceStock != null && sourceStock.getQuantityAvailable() != null
                    ? sourceStock.getQuantityAvailable() : 0;
            if (available < item.getQuantity()) {
                throw new ConflictException("Insufficient stock at source for batch " + item.getMedicineBatches().getBatchNumber());
            }

            sourceStock.setQuantityAvailable(available - item.getQuantity());
            sourceStock.setLastStockDate(today);
            stockRepo.save(sourceStock);

            movementsRepo.save(StockMovements.builder()
                    .medicineBatches(item.getMedicineBatches())
                    .branch(sourceBranch)
                    .user(approvedBy)
                    .movementType(StockMovements.MovementType.TRANSFER)
                    .referenceType("STOCK_TRANSFER_OUT")
                    .referenceId(st.getId())
                    .movementDate(today)
                    .quantity(item.getQuantity())
                    .build());
        }

        st.setApprovedBy(approvedBy);
        st.setTransferDate(today);
        st.setStatus(StockTransfer.Status.IN_TRANSIT.name());
        st = repo.save(st);
        return StockTransferResponseDto.from(st);
    }

    public StockTransferResponseDto receiveTransfer(UUID transferId) {
        StockTransfer st = repo.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", transferId));
        if (!StockTransfer.Status.IN_TRANSIT.name().equals(st.getStatus())) {
            throw new BadRequestException("Only IN_TRANSIT transfers can be received");
        }

        User receivedBy = current.user();
        Branch destBranch = st.getDestBranch();
        LocalDate today = LocalDate.now();

        for (StockTransferItem item : st.getItems()) {
            MedicineBatches batch = item.getMedicineBatches();
            int receivedQty = item.getQuantity();

            Stock destStock = stockRepo.findByBranchIdAndMedicineBatchesId(destBranch.getId(), batch.getId())
                    .orElseGet(() -> stockRepo.save(Stock.builder()
                            .branch(destBranch)
                            .medicineBatches(batch)
                            .quantityAvailable(0)
                            .reservedQuantity(0)
                            .minimumStock(0)
                            .reorderLevel(batch.getMedicine() != null ? batch.getMedicine().getReorderLevel() : 0)
                            .build()));

            destStock.setQuantityAvailable((destStock.getQuantityAvailable() != null
                    ? destStock.getQuantityAvailable() : 0) + receivedQty);
            destStock.setLastStockDate(today);
            stockRepo.save(destStock);

            item.setReceivedQuantity(receivedQty);
            movementsRepo.save(StockMovements.builder()
                    .medicineBatches(batch)
                    .branch(destBranch)
                    .user(receivedBy)
                    .movementType(StockMovements.MovementType.TRANSFER)
                    .referenceType("STOCK_TRANSFER_IN")
                    .referenceId(st.getId())
                    .movementDate(today)
                    .quantity(receivedQty)
                    .build());
        }

        st.setReceivedBy(receivedBy);
        st.setReceivedDate(today);
        st.setStatus(StockTransfer.Status.RECEIVED.name());
        st = repo.save(st);
        return StockTransferResponseDto.from(st);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponseDto> getAll(Pageable pageable) {
        Branch branch = current.branch();
        return repo.findBySourceBranchIdOrDestBranchIdOrderByCreatedAtDesc(branch.getId(), branch.getId(), pageable)
                .map(StockTransferResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponseDto> getTransfersOut(Pageable pageable) {
        Branch branch = current.branch();
        return repo.findBySourceBranchIdOrderByCreatedAtDesc(branch.getId(), pageable)
                .map(StockTransferResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponseDto> getTransfersIn(Pageable pageable) {
        Branch branch = current.branch();
        return repo.findByDestBranchIdOrderByCreatedAtDesc(branch.getId(), pageable)
                .map(StockTransferResponseDto::from);
    }

    @Transactional(readOnly = true)
    public StockTransferResponseDto getById(UUID id) {
        StockTransfer st = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", id));
        return StockTransferResponseDto.from(st);
    }
}
