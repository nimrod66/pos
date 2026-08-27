package com.example.pos.procurement.goodsreceived.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedResponseDto;
import com.example.pos.procurement.goodsreceived.model.GRNLine;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.goodsreceived.repository.GRNLineRepository;
import com.example.pos.procurement.goodsreceived.repository.GoodsReceivedNotesRepository;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
    private final AuthenticatedUserContext current;
    private final ObjectMapper objectMapper;

    public GoodsReceivedNotesService(GoodsReceivedNotesRepository repo,
                                     GRNLineRepository lineRepo,
                                     PurchaseOrdersRepository poRepo,
                                     SupplierRepository supplierRepo,
                                     MedicineRepository medicineRepo,
                                     MedicineBatchesRepository batchRepo,
                                     StockRepository stockRepo,
                                     StockMovementsRepository movementsRepo,
                                     IdempotencyKeyRepository idempotencyRepo,
                                     AuthenticatedUserContext current,
                                     ObjectMapper objectMapper) {
        this.repo = repo;
        this.lineRepo = lineRepo;
        this.poRepo = poRepo;
        this.supplierRepo = supplierRepo;
        this.medicineRepo = medicineRepo;
        this.batchRepo = batchRepo;
        this.stockRepo = stockRepo;
        this.movementsRepo = movementsRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.current = current;
        this.objectMapper = objectMapper;
    }

    @Auditable(action = "RECEIVE_STOCK", entity = "GoodsReceivedNote")
    public GoodsReceivedNotes receive(GoodsReceivedRequestDto dto, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        User receiver = current.user();
        Branch branch = receiver.getBranch();
        Pharmacy pharmacy = branch.getPharmacy();
        String requestHash = hashRequest(dto);

        var existing = idempotencyRepo.findByPharmacyIdAndIdempotencyKey(pharmacy.getId(), idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            if (!requestHash.equals(key.getRequestHash())) {
                throw new ConflictException("Idempotency key already used with different payload",
                        "IDEMPOTENCY_KEY_REUSED");
            }
            if (key.getStatus() == IdempotencyKey.Status.COMPLETED
                    && "GOODS_RECEIVED".equals(key.getResourceType()) && key.getResourceId() != null) {
                return repo.findByIdAndBranchId(UUID.fromString(key.getResourceId()), branch.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("GRN", key.getResourceId()));
            }
            throw new ConflictException("The receiving request is already in progress",
                    "IDEMPOTENCY_IN_PROGRESS");
        }

        IdempotencyKey key = IdempotencyKey.builder()
                .pharmacy(pharmacy)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .resourceType("GOODS_RECEIVED")
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .build();
        key = idempotencyRepo.save(key);

        Suppliers supplier = supplierRepo.findByIdAndPharmacyId(dto.getSupplierId(), pharmacy.getId())
                .filter(value -> value.getStatus() == Suppliers.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active supplier", dto.getSupplierId()));

        PurchaseOrders po = resolvePurchaseOrder(dto, supplier, branch);
        LocalDateTime receivedAt = dto.getReceivedAt() != null ? dto.getReceivedAt() : LocalDateTime.now();
        if (receivedAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BadRequestException("Received time cannot be in the future", "INVALID_RECEIVED_AT");
        }

        GoodsReceivedNotes grn = GoodsReceivedNotes.builder()
                .supplier(supplier)
                .supplierInvoiceNumber(trimToNull(dto.getSupplierInvoiceNumber()))
                .purchaseOrders(po)
                .branch(branch)
                .receivedBy(receiver)
                .receivedAt(receivedAt)
                .remarks(trimToNull(dto.getRemarks()))
                .idempotencyKey(idempotencyKey)
                .build();
        grn = repo.save(grn);

        for (GoodsReceivedRequestDto.GRNLineDto lineDto : dto.getLines()) {
            receiveLine(grn, po, branch, receiver, lineDto, receivedAt.toLocalDate());
        }

        if (po != null) {
            po.setStatus(allPOLinesFulfilled(po)
                    ? PurchaseOrders.Status.DELIVERED : PurchaseOrders.Status.IN_PROGRESS);
            po.setDeliveryDate(po.getStatus() == PurchaseOrders.Status.DELIVERED
                    ? LocalDateTime.now() : null);
            poRepo.save(po);
        }

        key.setResourceId(grn.getId().toString());
        key.setStatus(IdempotencyKey.Status.COMPLETED);
        grn.setIdempotencyKey(idempotencyKey);
        idempotencyRepo.save(key);
        repo.flush();

        log.info("GRN created: branch={} supplier={} invoice={} lines={}", branch.getId(),
                supplier.getId(), dto.getSupplierInvoiceNumber(), dto.getLines().size());
        return grn;
    }

    private PurchaseOrders resolvePurchaseOrder(GoodsReceivedRequestDto dto,
                                                 Suppliers supplier,
                                                 Branch branch) {
        if (dto.getPurchaseOrdersId() == null) return null;

        PurchaseOrders po = poRepo.findForUpdateById(dto.getPurchaseOrdersId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", dto.getPurchaseOrdersId()));
        if (po.getBranch() == null || !branch.getId().equals(po.getBranch().getId())) {
            throw new ForbiddenException("The purchase order belongs to another branch");
        }
        if (po.getSupplier() == null || !supplier.getId().equals(po.getSupplier().getId())) {
            throw new BadRequestException("Supplier does not match the purchase order",
                    "PURCHASE_ORDER_SUPPLIER_MISMATCH");
        }
        if (po.getStatus() == PurchaseOrders.Status.DELIVERED
                || po.getStatus() == PurchaseOrders.Status.FAILED) {
            throw new ConflictException("The purchase order cannot receive more stock",
                    "PURCHASE_ORDER_NOT_RECEIVABLE");
        }
        return po;
    }

    private void receiveLine(GoodsReceivedNotes grn,
                             PurchaseOrders po,
                             Branch branch,
                             User receiver,
                             GoodsReceivedRequestDto.GRNLineDto lineDto,
                             LocalDate receivedDate) {
        Medicine medicine = medicineRepo.findByIdAndPharmacyId(
                        lineDto.getMedicineId(), branch.getPharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", lineDto.getMedicineId()));
        validatePurchaseOrderLine(po, lineDto, medicine);

        if (medicine.isTrackExpiry()) {
            if (lineDto.getExpiryDate() == null) {
                throw new BadRequestException("Expiry date is required for " + medicine.getBrandName(),
                        "EXPIRY_DATE_REQUIRED");
            }
            if (!lineDto.getExpiryDate().isAfter(receivedDate)) {
                throw new BadRequestException("Expired stock cannot be received", "BATCH_EXPIRED");
            }
        }

        String batchNumber = lineDto.getBatchNumber().trim().toUpperCase(Locale.ROOT);
        MedicineBatches batch = batchRepo.findByBatchNumberAndMedicineId(batchNumber, medicine.getId())
                .orElseGet(() -> batchRepo.save(MedicineBatches.builder()
                        .medicine(medicine)
                        .batchNumber(batchNumber)
                        .expirationDate(lineDto.getExpiryDate())
                        .buyingPrice(lineDto.getUnitCost())
                        .sellingPrice(medicine.getSellingPrice())
                        .initialQuantity(0)
                        .build()));

        if (batch.getExpirationDate() != null && lineDto.getExpiryDate() != null
                && !batch.getExpirationDate().equals(lineDto.getExpiryDate())) {
            throw new ConflictException("Batch number already exists with a different expiry date",
                    "BATCH_DETAILS_CONFLICT");
        }

        int previousQuantity = batch.getInitialQuantity() != null ? batch.getInitialQuantity() : 0;
        BigDecimal previousCost = batch.getBuyingPrice() != null ? batch.getBuyingPrice() : BigDecimal.ZERO;

        // Convert buying units to selling units if packSize is set
        int receivedSellingUnits = lineDto.getQuantity();
        if (medicine.getPackSize() != null && medicine.getPackSize() > 0
                && medicine.getBuyingUnit() != null
                && !medicine.getBuyingUnit().getId().equals(medicine.getUnit() != null ? medicine.getUnit().getId() : null)) {
            receivedSellingUnits = lineDto.getQuantity() * medicine.getPackSize();
        }

        int combinedQuantity = previousQuantity + receivedSellingUnits;
        BigDecimal weightedCost = previousCost.multiply(BigDecimal.valueOf(previousQuantity))
                .add(lineDto.getUnitCost().multiply(BigDecimal.valueOf(lineDto.getQuantity())))
                .divide(BigDecimal.valueOf(combinedQuantity), 2, RoundingMode.HALF_UP);
        batch.setInitialQuantity(combinedQuantity);
        batch.setBuyingPrice(weightedCost);
        batchRepo.save(batch);

        GRNLine line = GRNLine.builder()
                .goodsReceivedNotes(grn)
                .medicine(medicine)
                .batch(batch)
                .batchNumber(batchNumber)
                .expiryDate(lineDto.getExpiryDate())
                .quantity(receivedSellingUnits)
                .unitCost(lineDto.getUnitCost())
                .purchaseOrderLineId(lineDto.getPurchaseOrderLineId())
                .build();
        lineRepo.save(line);
        grn.getLines().add(line);

        Stock stock = stockRepo.findForUpdate(branch.getId(), batch.getId())
                .orElseGet(() -> Stock.builder()
                        .branch(branch)
                        .medicineBatches(batch)
                        .quantityAvailable(0)
                        .reservedQuantity(0)
                        .minimumStock(0)
                        .reorderLevel(medicine.getReorderLevel())
                        .build());
        stock.setQuantityAvailable((stock.getQuantityAvailable() != null
                ? stock.getQuantityAvailable() : 0) + receivedSellingUnits);
        stock.setLastStockDate(receivedDate);
        stockRepo.save(stock);

        movementsRepo.save(StockMovements.builder()
                .medicineBatches(batch)
                .branch(branch)
                .user(receiver)
                .movementType(StockMovements.MovementType.PURCHASE)
                .referenceType("GOODS_RECEIVED")
                .referenceId(grn.getId())
                .movementDate(receivedDate)
                .quantity(receivedSellingUnits)
                .build());
    }

    private void validatePurchaseOrderLine(PurchaseOrders po,
                                           GoodsReceivedRequestDto.GRNLineDto lineDto,
                                           Medicine medicine) {
        if (po == null) {
            if (lineDto.getPurchaseOrderLineId() != null) {
                throw new BadRequestException("A direct GRN cannot reference a purchase-order line");
            }
            return;
        }
        if (lineDto.getPurchaseOrderLineId() == null) {
            throw new BadRequestException("Purchase-order line ID is required",
                    "PURCHASE_ORDER_LINE_REQUIRED");
        }

        PurchaseOrderItems poLine = po.getPurchaseOrderItems().stream()
                .filter(candidate -> candidate.getId().equals(lineDto.getPurchaseOrderLineId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Line is not part of the purchase order",
                        "PURCHASE_ORDER_LINE_MISMATCH"));
        if (poLine.getMedicine() == null || !medicine.getId().equals(poLine.getMedicine().getId())) {
            throw new BadRequestException("Medicine does not match the purchase-order line",
                    "PURCHASE_ORDER_MEDICINE_MISMATCH");
        }
        long alreadyReceived = lineRepo.sumQuantityByPurchaseOrderLineId(poLine.getId());
        if (alreadyReceived + lineDto.getQuantity() > poLine.getQuantity()) {
            throw new ConflictException("Received quantity exceeds the outstanding purchase-order quantity",
                    "PURCHASE_ORDER_OVER_RECEIPT");
        }
    }

    private boolean allPOLinesFulfilled(PurchaseOrders po) {
        return !po.getPurchaseOrderItems().isEmpty()
                && po.getPurchaseOrderItems().stream().allMatch(line ->
                lineRepo.sumQuantityByPurchaseOrderLineId(line.getId()) >= line.getQuantity());
    }

    @Transactional(readOnly = true)
    public Page<GoodsReceivedResponseDto> getByPurchaseOrder(UUID poId, Pageable pageable) {
        Branch branch = current.branch();
        PurchaseOrders po = poRepo.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", poId));
        if (po.getBranch() == null || !branch.getId().equals(po.getBranch().getId())) {
            throw new ForbiddenException("The purchase order belongs to another branch");
        }
        return repo.findByPurchaseOrdersIdAndBranchId(poId, branch.getId(), pageable)
                .map(GoodsReceivedResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<GoodsReceivedResponseDto> getAll(Pageable pageable) {
        return repo.findByBranchId(current.branch().getId(), pageable)
                .map(GoodsReceivedResponseDto::from);
    }

    @Transactional(readOnly = true)
    public GoodsReceivedResponseDto getById(UUID id) {
        GoodsReceivedNotes grn = repo.findByIdAndBranchId(id, current.branch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceivedNotes", id));
        return GoodsReceivedResponseDto.from(grn);
    }

    private void validateIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required", "IDEMPOTENCY_KEY_REQUIRED");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Idempotency-Key must be a UUID", "INVALID_IDEMPOTENCY_KEY");
        }
    }

    private String hashRequest(GoodsReceivedRequestDto dto) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(dto);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not fingerprint receiving request", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
