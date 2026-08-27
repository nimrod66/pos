package com.example.pos.inventory.stockcount.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockcount.dto.StockCountRequestDto;
import com.example.pos.inventory.stockcount.dto.StockCountResponseDto;
import com.example.pos.inventory.stockcount.model.StockCount;
import com.example.pos.inventory.stockcount.model.StockCountItem;
import com.example.pos.inventory.stockcount.repository.StockCountRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
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
public class StockCountService {

    private final StockCountRepository repo;
    private final StockRepository stockRepo;
    private final MedicineBatchesRepository batchRepo;
    private final StockMovementsRepository movementsRepo;
    private final AuthenticatedUserContext current;

    public StockCountService(StockCountRepository repo, StockRepository stockRepo,
                             MedicineBatchesRepository batchRepo,
                             StockMovementsRepository movementsRepo,
                             AuthenticatedUserContext current) {
        this.repo = repo;
        this.stockRepo = stockRepo;
        this.batchRepo = batchRepo;
        this.movementsRepo = movementsRepo;
        this.current = current;
    }

    public StockCountResponseDto createCount(StockCountRequestDto dto) {
        Branch branch = current.branch();
        if (repo.findByBranchIdAndCountDate(branch.getId(), dto.getCountDate()).isPresent()) {
            throw new ConflictException("A stock count already exists for this date at this branch");
        }

        User countedBy = current.user();
        StockCount sc = StockCount.builder()
                .branch(branch)
                .countDate(dto.getCountDate())
                .countedBy(countedBy)
                .status(StockCount.Status.DRAFT.name())
                .remarks(dto.getRemarks())
                .build();
        sc = repo.save(sc);

        if (dto.getItems() != null) {
            for (StockCountRequestDto.StockCountItemDto itemDto : dto.getItems()) {
                MedicineBatches batch = batchRepo.findById(itemDto.getMedicineBatchesId())
                        .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", itemDto.getMedicineBatchesId()));

                Stock stock = stockRepo.findByBranchIdAndMedicineBatchesId(branch.getId(), batch.getId())
                        .orElse(null);
                int systemQty = stock != null && stock.getQuantityAvailable() != null
                        ? stock.getQuantityAvailable() : 0;

                StockCountItem item = StockCountItem.builder()
                        .stockCount(sc)
                        .medicineBatches(batch)
                        .systemQuantity(systemQty)
                        .countedQuantity(itemDto.getCountedQuantity())
                        .remarks(itemDto.getRemarks())
                        .build();
                sc.getItems().add(item);
            }
            sc.setStatus(StockCount.Status.COUNTED.name());
        }

        sc = repo.save(sc);
        return StockCountResponseDto.from(sc);
    }

    public StockCountResponseDto reconcileCount(UUID countId) {
        StockCount sc = repo.findById(countId)
                .orElseThrow(() -> new ResourceNotFoundException("StockCount", countId));
        Branch branch = current.branch();
        if (!sc.getBranch().getId().equals(branch.getId())) {
            throw new BadRequestException("Stock count belongs to another branch");
        }
        if (!StockCount.Status.COUNTED.name().equals(sc.getStatus())) {
            throw new BadRequestException("Only COUNTED stock counts can be reconciled");
        }

        User reviewedBy = current.user();
        sc.setReviewedBy(reviewedBy);

        LocalDate today = LocalDate.now();
        for (StockCountItem item : sc.getItems()) {
            if (item.getCountedQuantity() == null) continue;
            int variance = item.getCountedQuantity() - item.getSystemQuantity();
            if (variance == 0) continue;

            Stock stock = stockRepo.findByBranchIdAndMedicineBatchesId(branch.getId(), item.getMedicineBatches().getId())
                    .orElse(null);
            if (stock == null) continue;

            stock.setQuantityAvailable(item.getCountedQuantity());
            stock.setLastStockDate(today);
            stockRepo.save(stock);

            StockMovements.MovementType movementType = variance > 0
                    ? StockMovements.MovementType.ADJUSTMENT
                    : StockMovements.MovementType.ADJUSTMENT;
            movementsRepo.save(StockMovements.builder()
                    .medicineBatches(item.getMedicineBatches())
                    .branch(branch)
                    .user(reviewedBy)
                    .movementType(movementType)
                    .referenceType("STOCK_COUNT")
                    .referenceId(sc.getId())
                    .movementDate(today)
                    .quantity(Math.abs(variance))
                    .build());
        }

        sc.setStatus(StockCount.Status.RECONCILED.name());
        sc = repo.save(sc);
        return StockCountResponseDto.from(sc);
    }

    @Transactional(readOnly = true)
    public Page<StockCountResponseDto> getAll(Pageable pageable) {
        Branch branch = current.branch();
        return repo.findByBranchIdOrderByCountDateDesc(branch.getId(), pageable)
                .map(StockCountResponseDto::from);
    }

    @Transactional(readOnly = true)
    public StockCountResponseDto getById(UUID id) {
        StockCount sc = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockCount", id));
        return StockCountResponseDto.from(sc);
    }
}
