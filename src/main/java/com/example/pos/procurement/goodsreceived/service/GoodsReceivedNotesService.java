package com.example.pos.procurement.goodsreceived.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.model.GRNLine;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.goodsreceived.repository.GRNLineRepository;
import com.example.pos.procurement.goodsreceived.repository.GoodsReceivedNotesRepository;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GoodsReceivedNotesService {

    private static final Logger log = LoggerFactory.getLogger(GoodsReceivedNotesService.class);

    private final GoodsReceivedNotesRepository repo;
    private final GRNLineRepository lineRepo;
    private final PurchaseOrdersRepository poRepo;
    private final SupplierRepository supplierRepo;
    private final MedicineRepository medicineRepo;
    private final MedicineBatchesRepository batchRepo;
    private final StockRepository stockRepo;
    private final StockMovementsRepository movementsRepo;
    private final IdempotencyKeyRepository idempotencyRepo;

    public GoodsReceivedNotesService(GoodsReceivedNotesRepository repo,
                                     GRNLineRepository lineRepo,
                                     PurchaseOrdersRepository poRepo,
                                     SupplierRepository supplierRepo,
                                     MedicineRepository medicineRepo,
                                     MedicineBatchesRepository batchRepo,
                                     StockRepository stockRepo,
                                     StockMovementsRepository movementsRepo,
                                     IdempotencyKeyRepository idempotencyRepo) {
        this.repo = repo;
        this.lineRepo = lineRepo;
        this.poRepo = poRepo;
        this.supplierRepo = supplierRepo;
        this.medicineRepo = medicineRepo;
        this.batchRepo = batchRepo;
        this.stockRepo = stockRepo;
        this.movementsRepo = movementsRepo;
        this.idempotencyRepo = idempotencyRepo;
    }

    public GoodsReceivedNotes receive(GoodsReceivedRequestDto dto) {
        String idempotencyKey = dto.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKey ik = existing.get();
                String requestHash = hashRequest(dto);
                if (!requestHash.equals(ik.getRequestHash())) {
                    throw new ConflictException("Idempotency key already used with different payload");
                }
                if (ik.getStatus() == IdempotencyKey.Status.COMPLETED
                        && "GOODS_RECEIVED".equals(ik.getResourceType()) && ik.getResourceId() != null) {
                    return repo.findById(UUID.fromString(ik.getResourceId()))
                            .orElseThrow(() -> new ResourceNotFoundException("GRN", ik.getResourceId()));
                }
            }
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = hashRequest(dto);
            if (!idempotencyRepo.existsByIdempotencyKey(idempotencyKey)) {
                IdempotencyKey ik = IdempotencyKey.builder()
                        .idempotencyKey(idempotencyKey)
                        .requestHash(requestHash)
                        .resourceType("GOODS_RECEIVED")
                        .status(IdempotencyKey.Status.IN_PROGRESS)
                        .build();
                idempotencyRepo.save(ik);
            }
        }

        Suppliers supplier = supplierRepo.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", dto.getSupplierId()));

        PurchaseOrders po = null;
        if (dto.getPurchaseOrdersId() != null) {
            po = poRepo.findById(dto.getPurchaseOrdersId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", dto.getPurchaseOrdersId()));
        }

        GoodsReceivedNotes grn = GoodsReceivedNotes.builder()
                .supplier(supplier)
                .supplierInvoiceNumber(dto.getSupplierInvoiceNumber())
                .purchaseOrders(po)
                .receivedAt(dto.getReceivedAt() != null ? dto.getReceivedAt() : LocalDateTime.now())
                .remarks(dto.getRemarks())
                .idempotencyKey(idempotencyKey)
                .build();
        grn = repo.save(grn);

        for (var lineDto : dto.getLines()) {
            Medicine medicine = medicineRepo.findById(lineDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", lineDto.getMedicineId()));

            MedicineBatches batch = batchRepo
                    .findByBatchNumberAndMedicineId(lineDto.getBatchNumber(), lineDto.getMedicineId())
                    .orElseGet(() -> {
                MedicineBatches newBatch = MedicineBatches.builder()
                        .medicine(medicine)
                        .batchNumber(lineDto.getBatchNumber())
                        .expirationDate(lineDto.getExpiryDate())
                        .buyingPrice(lineDto.getUnitCost())
                        .sellingPrice(lineDto.getUnitCost().multiply(BigDecimal.valueOf(1.3)))
                        .initialQuantity(lineDto.getQuantity())
                        .build();
                return batchRepo.save(newBatch);
                    });

            GRNLine line = GRNLine.builder()
                    .goodsReceivedNotes(grn)
                    .medicine(medicine)
                    .batch(batch)
                    .batchNumber(lineDto.getBatchNumber())
                    .expiryDate(lineDto.getExpiryDate())
                    .quantity(lineDto.getQuantity())
                    .unitCost(lineDto.getUnitCost())
                    .purchaseOrderLineId(lineDto.getPurchaseOrderLineId())
                    .build();
            lineRepo.save(line);

            batch.setInitialQuantity(
                    (batch.getInitialQuantity() != null ? batch.getInitialQuantity() : 0) + lineDto.getQuantity());
            batchRepo.save(batch);

            stockRepo.findByMedicineBatchesId(batch.getId())
                    .ifPresentOrElse(stock -> {
                        stock.setQuantityAvailable(
                                (stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0)
                                        + lineDto.getQuantity());
                        stockRepo.save(stock);
                    }, () -> {
                        Stock newStock = Stock.builder()
                                .medicineBatches(batch)
                                .quantityAvailable(lineDto.getQuantity())
                                .minimumStock(0)
                                .build();
                        stockRepo.save(newStock);
                    });

            StockMovements movement = StockMovements.builder()
                    .medicineBatches(batch)
                    .movementType(StockMovements.MovementType.PURCHASE)
                    .referenceType("GOODS_RECEIVED")
                    .referenceId(grn.getId())
                    .movementDate(LocalDateTime.now().toLocalDate())
                    .quantity(lineDto.getQuantity())
                    .build();
            movementsRepo.save(movement);
        }

        if (po != null && allPOLinesFulfilled(po, dto)) {
            po.setStatus(PurchaseOrders.Status.DELIVERED);
            po.setDeliveryDate(LocalDateTime.now());
        } else if (po != null) {
            po.setStatus(PurchaseOrders.Status.DELIVERED);
        }
        if (po != null) poRepo.save(po);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            UUID grnId = grn.getId();
            idempotencyRepo.findByIdempotencyKey(idempotencyKey).ifPresent(ik -> {
                ik.setResourceId(grnId.toString());
                ik.setStatus(IdempotencyKey.Status.COMPLETED);
                idempotencyRepo.save(ik);
            });
        }

        log.info("GRN created: supplier={} invoice={} lines={}", supplier.getSupplierName(),
                dto.getSupplierInvoiceNumber(), dto.getLines().size());
        return grn;
    }

    @Transactional(readOnly = true)
    public Page<GoodsReceivedNotes> getByPurchaseOrder(UUID poId, Pageable pageable) {
        List<GoodsReceivedNotes> list = repo.findByPurchaseOrdersId(poId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<GoodsReceivedNotes> getAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public GoodsReceivedNotes getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("GoodsReceivedNotes", id));
    }

    private boolean allPOLinesFulfilled(PurchaseOrders po, GoodsReceivedRequestDto dto) {
        return true;
    }

    private String hashRequest(GoodsReceivedRequestDto dto) {
        try {
            String canonical = dto.getSupplierId() + "|" + dto.getSupplierInvoiceNumber() + "|"
                    + (dto.getPurchaseOrdersId() != null ? dto.getPurchaseOrdersId() : "");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return dto.getIdempotencyKey();
        }
    }
}
