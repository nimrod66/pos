package com.example.pos.sale.salereturns.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.cashtransactions.repository.CashTransactionsRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.saleitems.repository.SaleItemsRepository;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.salereturns.dto.SaleReturnRequestDto;
import com.example.pos.sale.salereturns.dto.SaleReturnResponseDto;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.repository.SaleReturnsRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import com.example.pos.user.users.model.User;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SaleReturnsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SaleReturnsService.class);
    private final SaleReturnsRepository returnsRepository;
    private final SalesRepository salesRepository;
    private final SaleItemsRepository saleItemsRepository;
    private final StockRepository stockRepository;
    private final StockMovementsRepository movementsRepository;
    private final StaffShiftsRepository shiftsRepository;
    private final CashDrawersRepository drawersRepository;
    private final CashTransactionsRepository cashTransactionsRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final AuthenticatedUserContext current;
    private final SyncService syncService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Value("${pos.sales.return-window-days:7}")
    private int returnWindowDays;

    public SaleReturnsService(SaleReturnsRepository returnsRepository,
                              SalesRepository salesRepository,
                              SaleItemsRepository saleItemsRepository,
                              StockRepository stockRepository,
                              StockMovementsRepository movementsRepository,
                              StaffShiftsRepository shiftsRepository,
                              CashDrawersRepository drawersRepository,
                              CashTransactionsRepository cashTransactionsRepository,
                              PaymentRepository paymentRepository,
                              IdempotencyKeyRepository idempotencyRepository,
                              AuthenticatedUserContext current,
                              SyncService syncService,
                              ObjectMapper objectMapper,
                              EntityManager entityManager) {
        this.returnsRepository = returnsRepository;
        this.salesRepository = salesRepository;
        this.saleItemsRepository = saleItemsRepository;
        this.stockRepository = stockRepository;
        this.movementsRepository = movementsRepository;
        this.shiftsRepository = shiftsRepository;
        this.drawersRepository = drawersRepository;
        this.cashTransactionsRepository = cashTransactionsRepository;
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.current = current;
        this.syncService = syncService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Auditable(action = "CREATE_SALE_RETURN", entity = "SaleReturn")
    public SaleReturns createReturn(SaleReturnRequestDto dto, String idempotencyHeader) {
        String idempotencyKey = validateIdempotencyKey(dto, idempotencyHeader);
        User user = current.user();
        Branch branch = current.branch();
        Pharmacy pharmacy = branch.getPharmacy();
        String requestHash = hashRequest(dto);

        acquireLock("RETURN:" + pharmacy.getId() + ":" + idempotencyKey);
        SaleReturns repeated = findRepeatedReturn(
                pharmacy.getId(), branch.getId(), idempotencyKey, requestHash);
        if (repeated != null) return repeated;

        IdempotencyKey key = idempotencyRepository.saveAndFlush(IdempotencyKey.builder()
                .pharmacy(pharmacy)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .resourceType("SALE_RETURN")
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .build());

        if (dto.getUserId() != null && !user.getId().equals(dto.getUserId())) {
            throw new ForbiddenException("A return cannot be processed as another user");
        }

        Sales sale = salesRepository.findForUpdateByIdAndBranchId(dto.getSaleId(), branch.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", dto.getSaleId()));
        validateSale(sale);

        StaffShifts shift = shiftsRepository.findForUpdateByUserIdAndStatus(
                        user.getId(), StaffShifts.Status.ACTIVE)
                .filter(value -> branch.getId().equals(value.getBranch().getId()))
                .orElseThrow(() -> new ConflictException(
                        "An active shift is required to process a return", "SHIFT_NOT_ACTIVE"));
        CashDrawers drawer = drawersRepository.findOpenForUpdateByShiftId(shift.getId())
                .orElseThrow(() -> new ConflictException(
                        "The active shift has no open cash drawer", "CASH_DRAWER_NOT_OPEN"));

        RefundMethod refundMethod = parseRefundMethod(dto.getRefundMethod());
        String refundReference = validateRefundReference(refundMethod, dto.getRefundReference());
        if (refundReference != null) {
            acquireLock("RETURN-REF:" + refundReference.toLowerCase(Locale.ROOT));
            if (returnsRepository.existsByRefundMethodAndRefundReferenceIgnoreCase(
                    refundMethod.name(), refundReference)) {
                throw new ConflictException("Refund reference has already been used",
                        "REFUND_REFERENCE_DUPLICATE");
            }
        }

        Map<UUID, SaleItems> saleItems = new HashMap<>();
        sale.getSaleItems().forEach(item -> saleItems.put(item.getId(), item));
        Set<UUID> requestedItems = new HashSet<>();
        List<ReturnAllocation> allocations = new ArrayList<>();
        SaleReturns saleReturn = SaleReturns.builder()
                .clientReturnId(dto.getClientReturnId())
                .sales(sale)
                .user(user)
                .branch(branch)
                .staffShift(shift)
                .reason(dto.getReason().trim())
                .returnDate(LocalDateTime.now())
                .status("COMPLETED")
                .refundAmount(BigDecimal.ZERO.setScale(2))
                .refundMethod(refundMethod.name())
                .refundReference(refundReference)
                .build();

        BigDecimal refundTotal = BigDecimal.ZERO;
        for (SaleReturnRequestDto.ReturnItemDto requested : dto.getItems()) {
            if (!requestedItems.add(requested.getSaleItemId())) {
                throw new BadRequestException("Each sale item may appear only once in a return",
                        "DUPLICATE_RETURN_ITEM");
            }
            SaleItems saleItem = saleItems.get(requested.getSaleItemId());
            if (saleItem == null) {
                throw new BadRequestException("Return item does not belong to the selected sale",
                        "RETURN_ITEM_MISMATCH");
            }
            MedicineBatches batch = saleItem.getMedicineBatches();
            if (requested.getMedicineBatchesId() != null
                    && !batch.getId().equals(requested.getMedicineBatchesId())) {
                throw new BadRequestException("Return batch does not match the original sale item",
                        "RETURN_BATCH_MISMATCH");
            }

            int previouslyReturned = saleItemsRepository.sumCompletedReturnQuantity(saleItem.getId());
            int remainingQuantity = saleItem.getQuantity() - previouslyReturned;
            if (requested.getQuantity() > remainingQuantity) {
                throw new ConflictException("Return quantity exceeds the unreturned sale quantity",
                        "RETURN_QUANTITY_EXCEEDED");
            }
            BigDecimal previousRefund = saleItemsRepository
                    .sumCompletedRefundAmount(saleItem.getId());
            BigDecimal lineRefund = calculateRefund(
                    saleItem, requested.getQuantity(), remainingQuantity, previousRefund);

            Stock stock = stockRepository.findForUpdate(branch.getId(), batch.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock for batch " + batch.getId()));
            stock.setQuantityQuarantined(value(stock.getQuantityQuarantined())
                    + requested.getQuantity());
            stock.setLastStockDate(LocalDateTime.now().toLocalDate());

            saleReturn.getSaleReturnItems().add(SaleReturnItems.builder()
                    .saleReturns(saleReturn)
                    .saleItems(saleItem)
                    .medicineBatches(batch)
                    .quantity(requested.getQuantity())
                    .refundAmount(lineRefund)
                    .disposition("QUARANTINE")
                    .build());
            allocations.add(new ReturnAllocation(batch, requested.getQuantity()));
            refundTotal = refundTotal.add(lineRefund);
        }

        refundTotal = money(refundTotal);
        if (refundTotal.signum() <= 0) {
            throw new BadRequestException("The calculated refund must be greater than zero",
                    "INVALID_REFUND_AMOUNT");
        }
        saleReturn.setRefundAmount(refundTotal);

        BigDecimal expectedDrawerCash = null;
        if (refundMethod == RefundMethod.CASH) {
            BigDecimal drawerCash = drawer.getOpeningBalance()
                    .add(paymentRepository.sumCompletedCashForShift(shift.getId()))
                    .add(cashTransactionsRepository.sumNetCashForDrawer(drawer.getId()));
            if (drawerCash.compareTo(refundTotal) < 0) {
                throw new ConflictException("The cash drawer does not contain enough cash for this refund",
                        "INSUFFICIENT_DRAWER_CASH");
            }
            expectedDrawerCash = drawerCash.subtract(refundTotal);
        }

        SaleReturns saved = returnsRepository.saveAndFlush(saleReturn);
        if (refundMethod == RefundMethod.CASH) {
            cashTransactionsRepository.save(CashTransactions.builder()
                    .cashDrawers(drawer)
                    .transactionType("CASH_OUT")
                    .amount(refundTotal)
                    .remarks("Cash refund for " + sale.getInvoiceNumber())
                    .referenceType("SALE_RETURN")
                    .referenceId(saved.getId().toString())
                    .build());
            drawer.setExpectedClosingBalance(expectedDrawerCash);
        }
        for (ReturnAllocation allocation : allocations) {
            movementsRepository.save(StockMovements.builder()
                    .medicineBatches(allocation.batch())
                    .branch(branch)
                    .user(user)
                    .movementType(StockMovements.MovementType.RETURN)
                    .referenceType("SALE_RETURN_QUARANTINE")
                    .referenceId(saved.getId())
                    .movementDate(saved.getReturnDate().toLocalDate())
                    .quantity(allocation.quantity())
                    .build());
        }

        key.setStatus(IdempotencyKey.Status.COMPLETED);
        key.setResourceId(saved.getId().toString());
        idempotencyRepository.save(key);
        writeSyncEvents(saved, allocations);
        return returnsRepository.findDetailedByIdAndBranchId(saved.getId(), branch.getId())
                .orElse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SaleReturnResponseDto> getReturnsBySale(UUID saleId, Pageable pageable) {
        salesRepository.findDetailedByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return returnsRepository.findBySalesIdAndBranchId(
                saleId, current.branchId(), pageable).map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public SaleReturns getReturnById(UUID id) {
        return returnsRepository.findDetailedByIdAndBranchId(id, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("SaleReturn", id));
    }

    public SaleReturnResponseDto toResponseDto(SaleReturns saleReturn) {
        SaleReturnResponseDto dto = SaleReturnResponseDto.from(saleReturn);
        if (saleReturn.getSaleReturnItems() != null) {
            dto.setItems(saleReturn.getSaleReturnItems().stream()
                    .map(item -> SaleReturnResponseDto.ReturnItemResponse.builder()
                            .id(item.getId())
                            .saleItemId(item.getSaleItems() != null
                                    ? item.getSaleItems().getId() : null)
                            .medicineBatchesId(item.getMedicineBatches() != null
                                    ? item.getMedicineBatches().getId() : null)
                            .batchNumber(item.getMedicineBatches() != null
                                    ? item.getMedicineBatches().getBatchNumber() : null)
                            .medicineName(item.getMedicineBatches() != null
                                    && item.getMedicineBatches().getMedicine() != null
                                    ? item.getMedicineBatches().getMedicine().getBrandName() : null)
                            .quantity(item.getQuantity())
                            .refundAmount(item.getRefundAmount())
                            .disposition(item.getDisposition())
                            .build())
                    .toList());
        }
        return dto;
    }

    private void validateSale(Sales sale) {
        if (sale.getSaleStatus() != Sales.SaleStatus.COMPLETED
                || sale.getPaymentStatus() != Sales.PaymentStatus.PAID) {
            throw new ConflictException("Only completed, paid sales can be returned",
                    "SALE_NOT_RETURNABLE");
        }
        LocalDateTime completedAt = sale.getCompletedAt() != null
                ? sale.getCompletedAt() : sale.getCreatedAt();
        if (returnWindowDays >= 0
                && completedAt.plusDays(returnWindowDays).isBefore(LocalDateTime.now())) {
            throw new ConflictException("The sale is outside the configured return window",
                    "RETURN_WINDOW_EXPIRED");
        }
    }

    private RefundMethod parseRefundMethod(String value) {
        try {
            return RefundMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BadRequestException("Refund method must be CASH or MPESA_MANUAL",
                    "INVALID_REFUND_METHOD");
        }
    }

    private String validateRefundReference(RefundMethod method, String value) {
        String clean = value == null || value.isBlank() ? null : value.trim();
        if (method == RefundMethod.MPESA_MANUAL && clean == null) {
            throw new BadRequestException("M-Pesa refunds require a transaction reference",
                    "REFUND_REFERENCE_REQUIRED");
        }
        return method == RefundMethod.CASH ? null : clean;
    }

    private BigDecimal calculateRefund(SaleItems item, int quantity,
                                       int remainingQuantity, BigDecimal previousRefund) {
        BigDecimal remainingAmount = money(item.getTotal().subtract(previousRefund));
        if (quantity == remainingQuantity) return remainingAmount;
        return money(item.getTotal()
                .divide(BigDecimal.valueOf(item.getQuantity()), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(quantity)));
    }

    private String validateIdempotencyKey(SaleReturnRequestDto dto, String header) {
        if (header == null || header.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required",
                    "IDEMPOTENCY_KEY_REQUIRED");
        }
        try {
            UUID headerId = UUID.fromString(header.trim());
            if (!headerId.equals(dto.getClientReturnId())) {
                throw new BadRequestException(
                        "Idempotency-Key must match clientReturnId",
                        "IDEMPOTENCY_KEY_MISMATCH");
            }
            return headerId.toString();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Idempotency-Key must be a UUID",
                    "INVALID_IDEMPOTENCY_KEY");
        }
    }

    private SaleReturns findRepeatedReturn(UUID pharmacyId, UUID branchId,
                                           String keyValue, String requestHash) {
        var existing = idempotencyRepository.findByPharmacyIdAndIdempotencyKey(
                pharmacyId, keyValue);
        if (existing.isEmpty()) return null;
        IdempotencyKey key = existing.get();
        if (!requestHash.equals(key.getRequestHash())) {
            throw new ConflictException("Idempotency key already used with a different payload",
                    "IDEMPOTENCY_KEY_REUSED");
        }
        if (key.getStatus() == IdempotencyKey.Status.COMPLETED
                && "SALE_RETURN".equals(key.getResourceType()) && key.getResourceId() != null) {
            return returnsRepository.findDetailedByIdAndBranchId(
                            UUID.fromString(key.getResourceId()), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "SaleReturn", UUID.fromString(key.getResourceId())));
        }
        if (key.getStatus() == IdempotencyKey.Status.IN_PROGRESS) {
            if (key.getCreatedAt() != null
                    && key.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusHours(1))) {
                log.warn("Deleting stale IN_PROGRESS return idempotency key {} (created {})",
                        keyValue, key.getCreatedAt());
                idempotencyRepository.delete(key);
                idempotencyRepository.flush();
                return null;
            }
        }
        throw new ConflictException("The return request is already in progress",
                "IDEMPOTENCY_IN_PROGRESS");
    }

    private void acquireLock(String value) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(?1)")
                .setParameter(1, lockId(value))
                .getSingleResult();
    }

    private long lockId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create return lock", exception);
        }
    }

    private String hashRequest(SaleReturnRequestDto dto) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(dto);
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not fingerprint return request", exception);
        }
    }

    private void writeSyncEvents(SaleReturns saleReturn, List<ReturnAllocation> allocations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("returnId", saleReturn.getId());
        payload.put("clientReturnId", saleReturn.getClientReturnId());
        payload.put("saleId", saleReturn.getSales().getId());
        payload.put("branchId", saleReturn.getBranch().getId());
        payload.put("refundAmount", saleReturn.getRefundAmount());
        payload.put("refundMethod", saleReturn.getRefundMethod());
        syncService.writeOutboxEvent(EventType.SALE_RETURNED, "SALE_RETURN",
                saleReturn.getId().toString(), json(payload));

        for (ReturnAllocation allocation : allocations) {
            Map<String, Object> stockPayload = new LinkedHashMap<>();
            stockPayload.put("returnId", saleReturn.getId());
            stockPayload.put("branchId", saleReturn.getBranch().getId());
            stockPayload.put("batchId", allocation.batch().getId());
            stockPayload.put("quantity", allocation.quantity());
            stockPayload.put("disposition", "QUARANTINE");
            syncService.writeOutboxEvent(EventType.STOCK_ADJUSTED, "STOCK",
                    saleReturn.getBranch().getId() + "-" + allocation.batch().getId(),
                    json(stockPayload));
        }
    }

    private String json(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize return event", exception);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private enum RefundMethod {
        CASH,
        MPESA_MANUAL
    }

    private record ReturnAllocation(MedicineBatches batch, int quantity) {
    }
}
