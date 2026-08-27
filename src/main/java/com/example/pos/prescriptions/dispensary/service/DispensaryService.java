package com.example.pos.prescriptions.dispensary.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.prescriptions.dispensary.dto.DispensaryRequestDto;
import com.example.pos.prescriptions.dispensary.model.Dispensary;
import com.example.pos.prescriptions.dispensary.repository.DispensaryRepository;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.prescriptions.prescriptionitems.repository.PrescriptionItemsRepository;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.prescriptions.prescriptions.repository.PrescriptionsRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DispensaryService {

    private final DispensaryRepository repo;
    private final MedicineBatchesRepository batchRepo;
    private final UserRepository userRepo;
    private final PrescriptionItemsRepository prescriptionItemsRepo;
    private final PrescriptionsRepository prescriptionsRepo;
    private final StockRepository stockRepo;
    private final StockMovementsRepository movementsRepo;
    private final AuthenticatedUserContext current;

    public DispensaryService(DispensaryRepository repo, MedicineBatchesRepository batchRepo,
                             UserRepository userRepo, PrescriptionItemsRepository prescriptionItemsRepo,
                             PrescriptionsRepository prescriptionsRepo, StockRepository stockRepo,
                             StockMovementsRepository movementsRepo, AuthenticatedUserContext current) {
        this.repo = repo;
        this.batchRepo = batchRepo;
        this.userRepo = userRepo;
        this.prescriptionItemsRepo = prescriptionItemsRepo;
        this.prescriptionsRepo = prescriptionsRepo;
        this.stockRepo = stockRepo;
        this.movementsRepo = movementsRepo;
        this.current = current;
    }

    public Dispensary dispense(DispensaryRequestDto dto) {
        MedicineBatches batch = batchRepo.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));
        PrescriptionItems pi = prescriptionItemsRepo.findById(dto.getPrescriptionItemsId())
                .orElseThrow(() -> new ResourceNotFoundException("PrescriptionItem", dto.getPrescriptionItemsId()));

        // Validate total dispensed does not exceed prescribed quantity
        int alreadyDispensed = repo.findByPrescriptionItemsId(pi.getId()).stream()
                .mapToInt(Dispensary::getDispensedQuantity)
                .sum();
        int requestedQty = dto.getDispensedQuantity();
        int remaining = pi.getQuantity() - alreadyDispensed;
        if (requestedQty <= 0) {
            throw new BadRequestException("Dispensed quantity must be positive");
        }
        if (requestedQty > remaining) {
            throw new ConflictException("Cannot dispense " + requestedQty
                    + ". Only " + remaining + " remaining on this prescription item");
        }

        // Validate and deduct stock
        com.example.pos.core.branch.model.Branch branch = current.branch();
        Stock stock = stockRepo.findByBranchIdAndMedicineBatchesId(branch.getId(), batch.getId())
                .orElseThrow(() -> new ConflictException("No stock record for this batch at this branch"));
        int available = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0;
        if (available < requestedQty) {
            throw new ConflictException("Insufficient stock. Available: " + available + ", requested: " + requestedQty);
        }

        // Deduct stock
        stock.setQuantityAvailable(available - requestedQty);
        stock.setLastStockDate(LocalDate.now());
        stockRepo.save(stock);

        // Record stock movement
        movementsRepo.save(StockMovements.builder()
                .medicineBatches(batch)
                .branch(branch)
                .user(user)
                .movementType(StockMovements.MovementType.DISPENSE)
                .referenceType("DISPENSARY")
                .referenceId(pi.getId())
                .movementDate(LocalDate.now())
                .quantity(requestedQty)
                .build());

        // Create dispensary record
        Dispensary dispensary = new Dispensary();
        dispensary.setMedicineBatches(batch);
        dispensary.setUser(user);
        dispensary.setPrescriptionItems(pi);
        dispensary.setDispensedQuantity(requestedQty);
        dispensary.setDispensingDate(LocalDateTime.now());
        Dispensary saved = repo.save(dispensary);

        // Auto-update prescription status if all items fully dispensed
        Prescriptions rx = pi.getPrescriptions();
        boolean allDispensed = rx.getPrescriptionItems().stream().allMatch(item -> {
            int totalDispensed = repo.findByPrescriptionItemsId(item.getId()).stream()
                    .mapToInt(Dispensary::getDispensedQuantity).sum();
            return totalDispensed >= item.getQuantity();
        });
        if (allDispensed && "ACTIVE".equals(rx.getStatus())) {
            rx.setStatus("DISPENSED");
            rx.setDispensedAt(LocalDateTime.now());
            prescriptionsRepo.save(rx);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Dispensary> getByBatch(UUID batchId, Pageable pageable) {
        List<Dispensary> list = repo.findByMedicineBatchesId(batchId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<Dispensary> getByUser(UUID userId, Pageable pageable) {
        List<Dispensary> list = repo.findByUserId(userId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Dispensary getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispensary", id));
    }
}
