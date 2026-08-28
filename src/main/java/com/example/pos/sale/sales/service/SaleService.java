package com.example.pos.sale.sales.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.model.CustomerTransaction;
import com.example.pos.customer.repository.CustomerRepository;
import com.example.pos.customer.repository.CustomerTransactionRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.repository.StockMovementsRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.pharmacy.regulatory.controlledrugs.repository.ControlledDrugsRepository;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.prescriptions.prescriptions.repository.PrescriptionsRepository;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.receipts.model.Receipts;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.sales.dto.SaleRequestDto;
import com.example.pos.sale.sales.dto.SaleResponseDto;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.security.auth.PermissionCodes;
import com.example.pos.sync.config.SyncProperties;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import com.example.pos.terminal.auth.TerminalContext;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.repository.StaffShiftsRepository;
import com.example.pos.user.users.model.User;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SaleService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String CURRENCY = "KES";

    private final SalesRepository salesRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final StockRepository stockRepository;
    private final StockMovementsRepository movementsRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final StaffShiftsRepository shiftsRepository;
    private final PaymentRepository paymentRepository;
    private final PrescriptionsRepository prescriptionsRepository;
    private final ControlledDrugsRepository controlledDrugsRepository;
    private final AuthenticatedUserContext current;
    private final TerminalConfig terminalConfig;
    private final SyncProperties syncProperties;
    private final SyncService syncService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final com.example.pos.core.systemsettings.service.SystemSettingsService settingsService;

    public SaleService(SalesRepository salesRepository,
                       CustomerRepository customerRepository,
                       CustomerTransactionRepository customerTransactionRepository,
                       StockRepository stockRepository,
                       StockMovementsRepository movementsRepository,
                       IdempotencyKeyRepository idempotencyRepository,
                       StaffShiftsRepository shiftsRepository,
                       PaymentRepository paymentRepository,
                       PrescriptionsRepository prescriptionsRepository,
                       ControlledDrugsRepository controlledDrugsRepository,
                       AuthenticatedUserContext current,
                       TerminalConfig terminalConfig,
                       SyncProperties syncProperties,
                       SyncService syncService,
                       ObjectMapper objectMapper,
                       EntityManager entityManager,
                       com.example.pos.core.systemsettings.service.SystemSettingsService settingsService) {
        this.salesRepository = salesRepository;
        this.customerRepository = customerRepository;
        this.customerTransactionRepository = customerTransactionRepository;
        this.stockRepository = stockRepository;
        this.movementsRepository = movementsRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.shiftsRepository = shiftsRepository;
        this.paymentRepository = paymentRepository;
        this.prescriptionsRepository = prescriptionsRepository;
        this.controlledDrugsRepository = controlledDrugsRepository;
        this.current = current;
        this.terminalConfig = terminalConfig;
        this.syncProperties = syncProperties;
        this.syncService = syncService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.settingsService = settingsService;
    }

    @Auditable(action = "CREATE_SALE", entity = "Sale")
    public Sales createSale(SaleRequestDto dto, String idempotencyHeader) {
        String idempotencyKey = validateIdempotencyKey(dto, idempotencyHeader);
        User cashier = current.user();
        Branch branch = cashier.getBranch();
        Pharmacy pharmacy = branch.getPharmacy();
        String requestHash = hashRequest(dto);

        acquireIdempotencyLock(pharmacy.getId(), idempotencyKey);
        Sales repeated = findRepeatedSale(pharmacy, branch, idempotencyKey, requestHash);
        if (repeated != null) return repeated;

        IdempotencyKey key = idempotencyRepository.saveAndFlush(IdempotencyKey.builder()
                .pharmacy(pharmacy)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .resourceType("SALE")
                .status(IdempotencyKey.Status.IN_PROGRESS)
                .build());

        StaffShifts shift = shiftsRepository.findForUpdateById(dto.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", dto.getShiftId()));
        validateShift(shift, cashier, branch);

        Customer customer = dto.getCustomerId() == null ? null
                : customerRepository.findByIdAndPharmacyId(dto.getCustomerId(), pharmacy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", dto.getCustomerId()));
        Prescriptions prescription = resolvePrescription(dto.getPrescriptionReferenceId());
        Map<UUID, Integer> prescriptionAllowance = prescriptionAllowance(prescription);
        boolean prescriptionUsed = false;
        LocalDateTime checkoutAt = LocalDateTime.now();

        Sales sale = Sales.builder()
                .clientSaleId(dto.getClientSaleId())
                .invoiceNumber(generateInvoiceNumber(dto.getClientSaleId(), checkoutAt))
                .branch(branch)
                .user(cashier)
                .shift(shift)
                .customer(customer)
                .prescription(prescription)
                .idempotencyKey(key)
                .saleStatus(Sales.SaleStatus.COMPLETED)
                .paymentStatus(Sales.PaymentStatus.PAID)
                .currency(CURRENCY)
                .note(trimToNull(dto.getNote()))
                .completedAt(checkoutAt)
                .terminalId(getEffectiveTerminalId())
                .synced(!syncProperties.isEnabled())
                .build();

        Set<UUID> lineIds = new HashSet<>();
        List<StockAllocation> allocations = new ArrayList<>();
        BigDecimal subtotal = money(BigDecimal.ZERO);
        BigDecimal taxTotal = money(BigDecimal.ZERO);
        BigDecimal grandTotal = money(BigDecimal.ZERO);
        BigDecimal discountTotal = money(BigDecimal.ZERO);

        for (SaleRequestDto.SaleItemDto line : dto.getItems()) {
            if (!lineIds.add(line.getLineId())) {
                throw new BadRequestException("Each sale line ID must be unique", "DUPLICATE_LINE_ID");
            }
            BigDecimal discountPercent = line.getDiscountPercent() == null
                    ? BigDecimal.ZERO : line.getDiscountPercent();
            if (discountPercent.signum() < 0 || discountPercent.compareTo(HUNDRED) > 0) {
                throw new BadRequestException(
                        "Line discount must be between 0 and 100 percent", "INVALID_DISCOUNT_PERCENT");
            }
            if (discountPercent.signum() > 0) {
                BigDecimal maxPercent = maxDiscountPercent();
                if (discountPercent.compareTo(maxPercent) > 0) {
                    throw new ConflictException(
                            "Line discounts above " + maxPercent.toPlainString()
                                    + "% are not allowed at this till",
                            "DISCOUNT_LIMIT_EXCEEDED");
                }
            }

            int requestedQuantity = exactQuantity(line.getQuantity());
            List<Stock> stocks = stockRepository.findSellableFefoForUpdate(
                    branch.getId(), line.getMedicineId(), checkoutAt.toLocalDate());
            if (stocks.isEmpty()) {
                throw new ConflictException("No sellable stock is available for this medicine",
                        "INSUFFICIENT_STOCK");
            }

            Medicine medicine = stocks.getFirst().getMedicineBatches().getMedicine();
            validateSellingUnit(line, medicine);
            if (medicine.isRequiresPrescription() || medicine.isControlledDrug()) {
                validatePrescription(medicine, prescription, requestedQuantity, prescriptionAllowance);
                prescriptionUsed = true;
            }
            validateRequestedBatch(line, stocks);

            int remaining = requestedQuantity;
            for (Stock stock : stocks) {
                if (remaining == 0) break;
                int available = stock.getQuantityAvailable() == null ? 0 : stock.getQuantityAvailable();
                if (available <= 0) continue;

                MedicineBatches batch = stock.getMedicineBatches();
                BigDecimal unitPrice = requiredPrice(medicine, line.getExpectedUnitPrice());
                int allocatedQuantity = Math.min(available, remaining);
                LineAmounts amounts = calculateLineAmounts(
                        unitPrice, allocatedQuantity, medicine.getTax(), discountPercent);

                SaleItems saleItem = SaleItems.builder()
                        .sales(sale)
                        .medicineBatches(batch)
                        .clientLineId(line.getLineId())
                        .quantity(allocatedQuantity)
                        .price(unitPrice)
                        .discount(amounts.discount())
                        .taxRate(amounts.taxRate())
                        .taxableAmount(amounts.taxableAmount())
                        .tax(amounts.tax())
                        .total(amounts.total())
                        .build();
                sale.getSaleItems().add(saleItem);

                stock.setQuantityAvailable(available - allocatedQuantity);
                stock.setLastStockDate(checkoutAt.toLocalDate());
                allocations.add(new StockAllocation(batch, allocatedQuantity));
                remaining -= allocatedQuantity;
                subtotal = subtotal.add(amounts.taxableAmount());
                taxTotal = taxTotal.add(amounts.tax());
                grandTotal = grandTotal.add(amounts.total());
                discountTotal = discountTotal.add(amounts.discount());
            }

            if (remaining > 0) {
                throw new ConflictException("Insufficient stock. Requested " + requestedQuantity
                        + " but only " + (requestedQuantity - remaining) + " is available",
                        "INSUFFICIENT_STOCK");
            }
        }

        subtotal = money(subtotal);
        taxTotal = money(taxTotal);
        grandTotal = money(grandTotal);
        discountTotal = money(discountTotal);
        PaymentTotals paymentTotals = buildPayments(sale, dto, grandTotal, checkoutAt);
        boolean pendingOnlinePayment = paymentTotals.pendingOnlinePayment();

        sale.setSubtotal(subtotal);
        sale.setDiscountTotal(discountTotal);
        sale.setTax(taxTotal);
        sale.setTotal(grandTotal);
        sale.setPaidTotal(paymentTotals.paidTotal());
        sale.setCashTendered(paymentTotals.cashTendered());
        sale.setChangeDue(paymentTotals.changeDue());
        sale.setAmountOwed(paymentTotals.amountOwed());
        sale.setSaleStatus(pendingOnlinePayment
                ? Sales.SaleStatus.SUSPENDED : Sales.SaleStatus.COMPLETED);
        sale.setPaymentStatus(pendingOnlinePayment
                ? Sales.PaymentStatus.IN_PROGRESS
                : paymentTotals.amountOwed().signum() > 0
                        ? Sales.PaymentStatus.NOT_PAID
                        : Sales.PaymentStatus.PAID);
        sale.setCompletedAt(pendingOnlinePayment ? null : checkoutAt);

        // Simple loyalty accrual: one point per full KES 100 spent.
        if (!pendingOnlinePayment && customer != null) {
            int earned = grandTotal.movePointLeft(2).intValue();
            if (earned > 0) {
                customer.setLoyaltyPoints(
                        (customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints())
                                + earned);
            }
        }

        if (!pendingOnlinePayment) {
            sale.getReceipts().add(Receipts.builder()
                    .sales(sale)
                    .receiptNumber(generateReceiptNumber(dto.getClientSaleId(), checkoutAt))
                    .printedDate(checkoutAt)
                    .build());
        }

        Sales saved = salesRepository.saveAndFlush(sale);
        for (StockAllocation allocation : allocations) {
            movementsRepository.save(StockMovements.builder()
                    .medicineBatches(allocation.batch())
                    .branch(branch)
                    .user(cashier)
                    .movementType(pendingOnlinePayment
                            ? StockMovements.MovementType.RESERVATION
                            : StockMovements.MovementType.SALE)
                    .referenceType("SALE")
                    .referenceId(saved.getId())
                    .movementDate(checkoutAt.toLocalDate())
                    .quantity(allocation.quantity())
                    .build());
            if (!pendingOnlinePayment) writeStockEvent(saved, branch, allocation);
        }

        if (prescriptionUsed && !pendingOnlinePayment) {
            prescription.setStatus("DISPENSED");
            prescription.setDispensedAt(checkoutAt);
        }

        // Auto-record controlled drug dispensing
        if (!pendingOnlinePayment) {
            for (StockAllocation allocation : allocations) {
                Medicine med = allocation.batch().getMedicine();
                if (med != null && med.isControlledDrug()) {
                    controlledDrugsRepository.save(ControlledDrugs.builder()
                            .medicine(med)
                            .prescriptions(prescription)
                            .user(cashier)
                            .quantityDispensed(allocation.quantity())
                            .medicineBatches(allocation.batch())
                            .saleItems(null)
                            .branch(branch)
                            .build());
                }
            }
        }

        // Record credit sale: update customer balance and create ledger entry
        if (!pendingOnlinePayment && paymentTotals.amountOwed().signum() > 0 && customer != null) {
            BigDecimal newBalance = customer.getBalance().add(paymentTotals.amountOwed());
            customer.setBalance(newBalance);
            customerRepository.save(customer);

            customerTransactionRepository.save(CustomerTransaction.builder()
                    .customer(customer)
                    .sale(saved)
                    .transactionType(CustomerTransaction.TransactionType.CREDIT_ISSUED.name())
                    .amount(paymentTotals.amountOwed())
                    .runningBalance(newBalance)
                    .description("Credit sale " + saved.getInvoiceNumber())
                    .reference(saved.getInvoiceNumber())
                    .recordedBy(cashier)
                    .build());
        }

        key.setResourceId(saved.getId().toString());
        key.setStatus(IdempotencyKey.Status.COMPLETED);
        idempotencyRepository.save(key);
        if (!pendingOnlinePayment) writeSaleEvent(saved);
        return saved;
    }

    public Sales finalizeOnlinePayment(UUID saleId) {
        Sales sale = salesRepository.findForUpdateByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        if (sale.getSaleStatus() == Sales.SaleStatus.COMPLETED) return sale;
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new ConflictException("The pending sale has already been cancelled",
                    "SALE_ALREADY_CANCELLED");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        sale.setPaymentStatus(Sales.PaymentStatus.PAID);
        sale.setSaleStatus(Sales.SaleStatus.COMPLETED);
        sale.setPaidTotal(sale.getTotal());
        sale.setCompletedAt(completedAt);
        if (sale.getReceipts().isEmpty()) {
            sale.getReceipts().add(Receipts.builder()
                    .sales(sale)
                    .receiptNumber(generateReceiptNumber(sale.getClientSaleId(), completedAt))
                    .printedDate(completedAt)
                    .build());
        }

        Map<UUID, StockMovements> reservations = new HashMap<>();
        movementsRepository.findByReferenceTypeAndReferenceId("SALE", sale.getId()).stream()
                .filter(movement -> movement.getMovementType()
                        == StockMovements.MovementType.RESERVATION)
                .forEach(movement -> reservations.put(
                        movement.getMedicineBatches().getId(), movement));
        for (SaleItems item : sale.getSaleItems()) {
            MedicineBatches batch = item.getMedicineBatches();
            StockMovements reservation = reservations.get(batch.getId());
            if (reservation != null) {
                reservation.setMovementType(StockMovements.MovementType.SALE);
                reservation.setMovementDate(completedAt.toLocalDate());
            }
            StockAllocation allocation = new StockAllocation(batch, item.getQuantity());
            writeStockEvent(sale, sale.getBranch(), allocation);
        }

        if (sale.getPrescription() != null) {
            sale.getPrescription().setStatus("DISPENSED");
            sale.getPrescription().setDispensedAt(completedAt);
        }
        Sales saved = salesRepository.saveAndFlush(sale);
        writeSaleEvent(saved);
        return saved;
    }

    public Sales failOnlinePayment(UUID saleId) {
        Sales sale = salesRepository.findForUpdateByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) return sale;
        if (sale.getSaleStatus() == Sales.SaleStatus.COMPLETED) {
            throw new ConflictException("A completed sale cannot be released",
                    "SALE_ALREADY_COMPLETED");
        }

        for (SaleItems item : sale.getSaleItems()) {
            Stock stock = stockRepository.findForUpdate(
                            sale.getBranch().getId(), item.getMedicineBatches().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock for batch", item.getMedicineBatches().getId()));
            stock.setQuantityAvailable(stock.getQuantityAvailable() + item.getQuantity());
        }
        movementsRepository.findByReferenceTypeAndReferenceId("SALE", sale.getId()).stream()
                .filter(movement -> movement.getMovementType()
                        == StockMovements.MovementType.RESERVATION)
                .forEach(movement -> movement.setMovementType(
                        StockMovements.MovementType.RESERVATION_RELEASE));

        sale.setPaymentStatus(Sales.PaymentStatus.NOT_PAID);
        sale.setSaleStatus(Sales.SaleStatus.CANCELLED);
        sale.setPaidTotal(money(BigDecimal.ZERO));
        sale.setCompletedAt(null);
        return salesRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public Page<Sales> getSalesByBranch(UUID branchId, Pageable pageable) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return salesRepository.findByBranchId(branch.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Sales getSaleById(UUID id) {
        return findDetailedSale(id, current.branch().getId());
    }

    @Auditable(action = "CANCEL_SALE", entity = "Sale")
    public Sales cancelSale(UUID id) {
        Sales sale = getSaleById(id);
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new ConflictException("Sale is already cancelled", "SALE_ALREADY_CANCELLED");
        }
        throw new ConflictException("Completed sales are immutable; use the returns workflow",
                "SALE_RETURN_REQUIRED");
    }

    public Sales suspendSale(UUID id) {
        getSaleById(id);
        throw new ConflictException("Only draft sales can be suspended", "SALE_NOT_DRAFT");
    }

    public Sales resumeSale(UUID id) {
        getSaleById(id);
        throw new ConflictException("Only draft sales can be resumed", "SALE_NOT_SUSPENDED");
    }

    public Sales overrideItemPrice(UUID saleId, UUID itemId, BigDecimal newPrice, String reason) {
        getSaleById(saleId);
        throw new ConflictException("Completed sale prices are immutable", "IMMUTABLE_SALE");
    }

    @Transactional(readOnly = true)
    public Sales getLastSaleByUserAndBranch(UUID userId, UUID branchId) {
        User user = current.user();
        Branch branch = user.getBranch();
        if (!user.getId().equals(userId) || !branch.getId().equals(branchId)) {
            throw new ForbiddenException("The requested sale is outside the active session");
        }
        return salesRepository.findTop1ByUserIdAndBranchIdOrderByCreatedAtDesc(userId, branchId)
                .map(sale -> findDetailedSale(sale.getId(), branchId))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Sales> getSuspendedSales(UUID branchId) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return salesRepository.findByBranchIdAndSaleStatus(branch.getId(), Sales.SaleStatus.SUSPENDED);
    }

    @Transactional(readOnly = true)
    public List<Sales> getSalesByBranchAndDate(UUID branchId, LocalDateTime start, LocalDateTime end) {
        current.requireBranch(branchId);
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BadRequestException("A valid sale date range is required", "INVALID_DATE_RANGE");
        }
        return salesRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end);
    }

    @Transactional(readOnly = true)
    public SaleResponseDto toResponseDto(Sales sale) {
        Sales detailed = findDetailedSale(sale.getId(), current.branch().getId());
        SaleResponseDto dto = SaleResponseDto.from(detailed);
        dto.setPrescriptionReferenceId(detailed.getPrescription() == null
                ? null : detailed.getPrescription().getId());
        dto.setCustomerId(detailed.getCustomer() == null ? null : detailed.getCustomer().getId());
        dto.setCustomerName(customerName(detailed.getCustomer()));
        dto.setCustomerKraPin(detailed.getCustomer() == null ? null : detailed.getCustomer().getKraPin());

        Map<UUID, SaleResponseDto.SaleItemResponse> lines = new LinkedHashMap<>();
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (SaleItems item : detailed.getSaleItems()) {
            MedicineBatches batch = item.getMedicineBatches();
            Medicine medicine = batch.getMedicine();
            SaleResponseDto.SaleItemResponse line = lines.computeIfAbsent(item.getClientLineId(), ignored ->
                    SaleResponseDto.SaleItemResponse.builder()
                            .id(item.getId())
                            .lineId(item.getClientLineId())
                            .medicineId(medicine.getId())
                            .medicineBatchesId(batch.getId())
                            .batchNumber(batch.getBatchNumber())
                            .medicineName(medicine.getBrandName())
                            .quantity(0)
                            .returnedQuantity(0)
                            .price(item.getPrice())
                            .unitPrice(item.getPrice())
                            .discount(money(BigDecimal.ZERO))
                            .taxRate(item.getTaxRate())
                            .taxableAmount(money(BigDecimal.ZERO))
                            .tax(money(BigDecimal.ZERO))
                            .total(money(BigDecimal.ZERO))
                            .lineTotal(money(BigDecimal.ZERO))
                            .allocations(new ArrayList<>())
                            .build());
            int returnedQuantity = item.getSaleReturnItems().stream()
                    .filter(returnItem -> "COMPLETED".equalsIgnoreCase(
                            returnItem.getSaleReturns().getStatus()))
                    .mapToInt(returnItem -> returnItem.getQuantity() == null
                            ? 0 : returnItem.getQuantity())
                    .sum();
            BigDecimal itemRefundTotal = item.getSaleReturnItems().stream()
                    .filter(returnItem -> "COMPLETED".equalsIgnoreCase(
                            returnItem.getSaleReturns().getStatus()))
                    .map(returnItem -> returnItem.getRefundAmount() == null
                            ? BigDecimal.ZERO : returnItem.getRefundAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            line.setQuantity(line.getQuantity() + item.getQuantity());
            line.setReturnedQuantity(line.getReturnedQuantity() + returnedQuantity);
            line.setDiscount(money(line.getDiscount().add(item.getDiscount())));
            line.setTaxableAmount(money(line.getTaxableAmount().add(item.getTaxableAmount())));
            line.setTax(money(line.getTax().add(item.getTax())));
            line.setTotal(money(line.getTotal().add(item.getTotal())));
            line.setLineTotal(line.getTotal());
            line.getAllocations().add(SaleResponseDto.BatchAllocationResponse.builder()
                    .saleItemId(item.getId())
                    .batchId(batch.getId())
                    .batchNumber(batch.getBatchNumber())
                    .quantity(item.getQuantity())
                    .build());
            refundTotal = refundTotal.add(itemRefundTotal);
        }
        dto.setItems(new ArrayList<>(lines.values()));
        dto.setRefundTotal(money(refundTotal));

        List<SaleResponseDto.PaymentResponse> payments = detailed.getPayment().stream()
                .map(payment -> SaleResponseDto.PaymentResponse.builder()
                        .id(payment.getId())
                        .paymentMethod(responsePaymentMethod(payment.getPaymentMethod()))
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .transactionReference(payment.getTransactionReference())
                        .merchantRequestId(payment.getMerchantRequestId())
                        .checkoutRequestId(payment.getCheckoutRequestId())
                        .paymentStatus(payment.getPaymentStatus())
                        .paymentDate(payment.getPaymentDate())
                        .build())
                .toList();
        dto.setPayments(payments);
        return dto;
    }

    private Sales findRepeatedSale(Pharmacy pharmacy,
                                   Branch branch,
                                   String idempotencyKey,
                                   String requestHash) {
        var existing = idempotencyRepository.findByPharmacyIdAndIdempotencyKey(
                pharmacy.getId(), idempotencyKey);
        if (existing.isEmpty()) return null;

        IdempotencyKey key = existing.get();
        if (!requestHash.equals(key.getRequestHash())) {
            throw new ConflictException("Idempotency key already used with a different payload",
                    "IDEMPOTENCY_KEY_REUSED");
        }
        if (key.getStatus() == IdempotencyKey.Status.COMPLETED
                && "SALE".equals(key.getResourceType()) && key.getResourceId() != null) {
            return findDetailedSale(UUID.fromString(key.getResourceId()), branch.getId());
        }
        throw new ConflictException("The checkout request is already in progress",
                "IDEMPOTENCY_IN_PROGRESS");
    }

    private void validateShift(StaffShifts shift, User user, Branch branch) {
        if (shift.getStatus() != StaffShifts.Status.ACTIVE) {
            throw new ConflictException("The cashier shift is not active", "SHIFT_NOT_ACTIVE");
        }
        if (shift.getUser() == null || !user.getId().equals(shift.getUser().getId())) {
            throw new ForbiddenException("The shift belongs to another user");
        }
        if (shift.getBranch() == null || !branch.getId().equals(shift.getBranch().getId())) {
            throw new ForbiddenException("The shift belongs to another branch");
        }
    }

    private Prescriptions resolvePrescription(UUID prescriptionId) {
        if (prescriptionId == null) return null;
        Prescriptions prescription = prescriptionsRepository.findDetailedByIdAndBranchId(
                        prescriptionId, current.branch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", prescriptionId));
        if (!"ACTIVE".equalsIgnoreCase(prescription.getStatus())) {
            throw new ConflictException("The prescription is not active", "PRESCRIPTION_NOT_ACTIVE");
        }
        return prescription;
    }

    private void validatePrescription(Medicine medicine,
                                      Prescriptions prescription,
                                      int requestedQuantity,
                                      Map<UUID, Integer> allowance) {
        if (prescription == null) {
            throw new BadRequestException("A valid prescription reference is required for "
                    + medicine.getBrandName(), "PRESCRIPTION_REQUIRED");
        }
        if (!current.hasAuthority(PermissionCodes.PRESCRIPTION_APPROVE)) {
            throw new ForbiddenException("A pharmacist must complete prescription sales");
        }
        int remaining = allowance.getOrDefault(medicine.getId(), 0);
        if (remaining < requestedQuantity) {
            throw new BadRequestException("Prescription does not authorize the requested quantity of "
                    + medicine.getBrandName(), "PRESCRIPTION_QUANTITY_EXCEEDED");
        }
        allowance.put(medicine.getId(), remaining - requestedQuantity);
    }

    private Map<UUID, Integer> prescriptionAllowance(Prescriptions prescription) {
        Map<UUID, Integer> allowance = new HashMap<>();
        if (prescription == null) return allowance;
        prescription.getPrescriptionItems().forEach(item -> {
            if (item.getMedicine() != null && item.getQuantity() != null) {
                allowance.merge(item.getMedicine().getId(), item.getQuantity(), Integer::sum);
            }
        });
        return allowance;
    }

    private void validateSellingUnit(SaleRequestDto.SaleItemDto line, Medicine medicine) {
        if (line.getSellingUnitId() == null) return;
        UUID sellingUnitId = line.getSellingUnitId();
        // Accept the medicine's base dispensing unit
        if (medicine.getUnit() != null && sellingUnitId.equals(medicine.getUnit().getId())) return;
        // Accept the medicine's buying unit (e.g., strip, box)
        if (medicine.getBuyingUnit() != null && sellingUnitId.equals(medicine.getBuyingUnit().getId())) return;
        // Accept any ancestor unit in the hierarchy by walking parent pointers
        com.example.pos.masterdata.units.model.Unit current = medicine.getUnit();
        while (current != null && current.getParentUnit() != null) {
            current = current.getParentUnit();
            if (sellingUnitId.equals(current.getId())) return;
        }
        throw new BadRequestException("Selling unit does not match the medicine or its unit hierarchy",
                "SELLING_UNIT_MISMATCH");
    }

    private void validateRequestedBatch(SaleRequestDto.SaleItemDto line, List<Stock> stocks) {
        if (line.getRequestedBatchId() == null) return;
        UUID fefoBatchId = stocks.getFirst().getMedicineBatches().getId();
        if (!line.getRequestedBatchId().equals(fefoBatchId)) {
            throw new ConflictException("The requested batch is not the current FEFO batch",
                    "FEFO_OVERRIDE_REQUIRED");
        }
    }

    private BigDecimal requiredPrice(Medicine medicine, BigDecimal expectedPrice) {
        if (medicine.getSellingPrice() == null) {
            throw new ConflictException("Medicine " + medicine.getBrandName() + " has no selling price",
                    "SELLING_PRICE_MISSING");
        }
        BigDecimal currentPrice = money(medicine.getSellingPrice());
        if (currentPrice.compareTo(money(expectedPrice)) != 0) {
            throw new ConflictException("The selling price changed to " + currentPrice,
                    "PRICE_CHANGED");
        }
        return currentPrice;
    }

    private LineAmounts calculateLineAmounts(BigDecimal unitPrice, int quantity,
                                             Tax taxCategory, BigDecimal discountPercent) {
        BigDecimal taxRate = taxCategory == null || !taxCategory.isActive()
                || taxCategory.getTaxRate() == null ? BigDecimal.ZERO : taxCategory.getTaxRate();
        if (taxRate.signum() < 0 || taxRate.compareTo(HUNDRED) > 0) {
            throw new ConflictException("The medicine has an invalid tax rate", "INVALID_TAX_RATE");
        }
        BigDecimal gross = money(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal discount = money(gross.multiply(discountPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        BigDecimal total = money(gross.subtract(discount));
        BigDecimal tax = taxRate.signum() == 0 ? money(BigDecimal.ZERO)
                : total.multiply(taxRate)
                .divide(HUNDRED.add(taxRate), 2, RoundingMode.HALF_UP);
        return new LineAmounts(taxRate, money(total.subtract(tax)), money(tax), total, discount);
    }

    private BigDecimal maxDiscountPercent() {
        String configured = settingsService.resolveSettingValue(
                "sale.max_discount_percent", current.branchId(), current.pharmacyId(), "20");
        try {
            BigDecimal value = new BigDecimal(configured.trim());
            if (value.signum() < 0) return BigDecimal.ZERO;
            return value.min(HUNDRED);
        } catch (NumberFormatException ex) {
            return new BigDecimal("20");
        }
    }

    private PaymentTotals buildPayments(Sales sale,
                                        SaleRequestDto dto,
                                        BigDecimal total,
                                        LocalDateTime completedAt) {
        BigDecimal submitted = money(BigDecimal.ZERO);
        BigDecimal paid = money(BigDecimal.ZERO);
        BigDecimal cashApplied = money(BigDecimal.ZERO);
        BigDecimal creditAmount = money(BigDecimal.ZERO);
        Set<String> manualReferences = new HashSet<>();
        boolean pendingOnlinePayment = false;

        for (SaleRequestDto.PaymentItemDto input : dto.getPayments()) {
            Payment.PaymentMethod method = requestPaymentMethod(input.getMethod());
            BigDecimal amount = money(input.getAmount());
            String reference = trimToNull(input.getReference());
            if (method == Payment.PaymentMethod.MPESA_MANUAL) {
                if (reference == null) {
                    throw new BadRequestException("M-Pesa reference is required",
                            "PAYMENT_REFERENCE_REQUIRED");
                }
                reference = reference.toUpperCase(Locale.ROOT);
                if (reference.length() > 100) {
                    throw new BadRequestException("M-Pesa reference is too long",
                            "INVALID_PAYMENT_REFERENCE");
                }
                if (!manualReferences.add(reference)) {
                    throw new BadRequestException("M-Pesa references must be unique within a sale",
                            "DUPLICATE_PAYMENT_REFERENCE");
                }
                acquireTransactionLock("mpesa:" + reference);
                if (paymentRepository.existsByTransactionReferenceIgnoreCase(reference)) {
                    throw new ConflictException("M-Pesa reference has already been used",
                            "PAYMENT_REFERENCE_REUSED");
                }
            } else if (method == Payment.PaymentMethod.M_PESA) {
                if (reference != null) {
                    throw new BadRequestException("STK Push payments cannot include a manual reference",
                            "INVALID_PAYMENT_REFERENCE");
                }
                pendingOnlinePayment = true;
            } else if (method == Payment.PaymentMethod.CREDIT) {
                String allowCredit = settingsService.resolveSettingValue(
                        "sale.allow_credit_sales", current.branchId(), current.pharmacyId(), "true");
                if (!Boolean.parseBoolean(allowCredit)) {
                    throw new BadRequestException("Credit sales are not enabled",
                            "CREDIT_SALES_DISABLED");
                }
                if (sale.getCustomer() == null) {
                    throw new BadRequestException("Credit sales require a customer to be selected",
                            "CREDIT_REQUIRES_CUSTOMER");
                }
                if (amount.compareTo(total) > 0) {
                    throw new BadRequestException("Credit amount cannot exceed the sale total",
                            "CREDIT_AMOUNT_EXCEEDS_TOTAL");
                }
                BigDecimal currentBalance = sale.getCustomer().getBalance() != null
                        ? sale.getCustomer().getBalance() : BigDecimal.ZERO;
                BigDecimal newOwed = total.subtract(submitted).subtract(amount).negate();
                BigDecimal projectedBalance = currentBalance.add(newOwed);
                if (sale.getCustomer().getCreditLimit() != null
                        && projectedBalance.compareTo(sale.getCustomer().getCreditLimit()) > 0) {
                    throw new BadRequestException("Credit limit exceeded. Current balance: "
                            + currentBalance + ", limit: " + sale.getCustomer().getCreditLimit(),
                            "CREDIT_LIMIT_EXCEEDED");
                }
            } else if (reference != null) {
                throw new BadRequestException("Cash payments cannot include a transaction reference",
                        "INVALID_PAYMENT_REFERENCE");
            }

            Payment payment = Payment.builder()
                    .sales(sale)
                    .paymentMethod(method)
                    .amount(amount)
                    .currency(CURRENCY)
                    .transactionReference(reference)
                    .paymentStatus(method == Payment.PaymentMethod.M_PESA
                            ? "PENDING" : "COMPLETED")
                    .paymentDate(method == Payment.PaymentMethod.M_PESA
                            ? null : completedAt)
                    .build();
            sale.getPayment().add(payment);
            submitted = submitted.add(amount);
            if (method != Payment.PaymentMethod.M_PESA) paid = paid.add(amount);
            if (method == Payment.PaymentMethod.CASH) cashApplied = cashApplied.add(amount);
            if (method == Payment.PaymentMethod.CREDIT) creditAmount = creditAmount.add(amount);
        }

        if (pendingOnlinePayment && dto.getPayments().size() != 1) {
            throw new BadRequestException("STK Push cannot be combined with another payment method",
                    "UNSUPPORTED_SPLIT_PAYMENT");
        }
        submitted = money(submitted);
        paid = money(paid);
        cashApplied = money(cashApplied);
        creditAmount = money(creditAmount);

        BigDecimal amountOwed = money(total.subtract(submitted));
        if (amountOwed.signum() < 0) {
            throw new BadRequestException("Total payments exceed the sale total",
                    "PAYMENTS_EXCEED_TOTAL");
        }

        if (amountOwed.signum() > 0 && creditAmount.signum() == 0) {
            throw new BadRequestException("Payments must equal the sale total of " + total,
                    "PAYMENT_TOTAL_MISMATCH");
        }

        BigDecimal tendered = dto.getCashTendered() == null
                ? money(BigDecimal.ZERO) : money(dto.getCashTendered());
        if (cashApplied.signum() > 0 && dto.getCashTendered() == null) {
            throw new BadRequestException("Cash tendered is required for cash payments",
                    "CASH_TENDERED_REQUIRED");
        }
        if (cashApplied.signum() == 0 && tendered.signum() != 0) {
            throw new BadRequestException("Cash tendered is only valid for cash payments",
                    "UNEXPECTED_CASH_TENDERED");
        }
        if (tendered.compareTo(cashApplied) < 0) {
            throw new BadRequestException("Cash tendered is less than the cash amount due",
                    "INSUFFICIENT_CASH_TENDERED");
        }
        return new PaymentTotals(
                paid, tendered, money(tendered.subtract(cashApplied)), pendingOnlinePayment, amountOwed);
    }

    private Payment.PaymentMethod requestPaymentMethod(String value) {
        String method = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (method) {
            case "CASH" -> Payment.PaymentMethod.CASH;
            case "MPESA", "MPESA_MANUAL" -> Payment.PaymentMethod.MPESA_MANUAL;
            case "M_PESA", "MPESA_STK" -> Payment.PaymentMethod.M_PESA;
            case "CREDIT" -> Payment.PaymentMethod.CREDIT;
            default -> throw new BadRequestException("Only CASH, MPESA_MANUAL, M_PESA, and CREDIT are supported",
                    "UNSUPPORTED_PAYMENT_METHOD");
        };
    }

    private String responsePaymentMethod(Payment.PaymentMethod method) {
        return method == Payment.PaymentMethod.MPESA_MANUAL ? "MPESA" : method.name();
    }

    private int exactQuantity(BigDecimal quantity) {
        try {
            int value = quantity.intValueExact();
            if (value <= 0) throw new ArithmeticException();
            return value;
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Sale quantities must be positive whole numbers",
                    "INVALID_QUANTITY");
        }
    }

    private String validateIdempotencyKey(SaleRequestDto dto, String header) {
        if (header == null || header.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required",
                    "IDEMPOTENCY_KEY_REQUIRED");
        }
        UUID key;
        try {
            key = UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Idempotency-Key must be a UUID",
                    "INVALID_IDEMPOTENCY_KEY");
        }
        if (!key.equals(dto.getClientSaleId())) {
            throw new BadRequestException("Idempotency-Key must match clientSaleId",
                    "IDEMPOTENCY_KEY_MISMATCH");
        }
        return key.toString();
    }

    private void acquireIdempotencyLock(UUID pharmacyId, String idempotencyKey) {
        acquireTransactionLock("checkout:" + pharmacyId + ":" + idempotencyKey);
    }

    private void acquireTransactionLock(String lockName) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(?1)")
                .setParameter(1, lockId(lockName))
                .getSingleResult();
    }

    private long lockId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create idempotency lock", ex);
        }
    }

    private String hashRequest(SaleRequestDto dto) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(dto);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not fingerprint checkout request", ex);
        }
    }

    private Sales findDetailedSale(UUID saleId, UUID branchId) {
        return salesRepository.findDetailedByIdAndBranchId(saleId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
    }

    private void writeStockEvent(Sales sale, Branch branch, StockAllocation allocation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("saleId", sale.getId());
        payload.put("branchId", branch.getId());
        payload.put("batchId", allocation.batch().getId());
        payload.put("quantity", allocation.quantity());
        syncService.writeOutboxEvent(EventType.STOCK_DEDUCTED, "STOCK",
                branch.getId() + "-" + allocation.batch().getId(), json(payload));
    }

    private void writeSaleEvent(Sales sale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("saleId", sale.getId());
        payload.put("clientSaleId", sale.getClientSaleId());
        payload.put("invoiceNumber", sale.getInvoiceNumber());
        payload.put("branchId", sale.getBranch().getId());
        payload.put("total", sale.getTotal());
        payload.put("currency", sale.getCurrency());
        payload.put("terminalId", sale.getTerminalId());
        syncService.writeOutboxEvent(EventType.SALE_CREATED, "SALE", sale.getUuid(), json(payload));
    }

    private String json(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize sync event", ex);
        }
    }

    private String generateInvoiceNumber(UUID clientSaleId, LocalDateTime completedAt) {
        return "SL-" + completedAt.toLocalDate().toString().replace("-", "") + "-"
                + clientSaleId.toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String generateReceiptNumber(UUID clientSaleId, LocalDateTime completedAt) {
        return "RC-" + completedAt.toLocalDate().toString().replace("-", "") + "-"
                + clientSaleId.toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String getEffectiveTerminalId() {
        String terminalId = TerminalContext.getCurrentTerminalId();
        return terminalId != null ? terminalId : terminalConfig.getTerminalId();
    }

    private String customerName(Customer customer) {
        if (customer == null) return null;
        String first = customer.getFirstName() == null ? "" : customer.getFirstName().trim();
        String last = customer.getLastName() == null ? "" : customer.getLastName().trim();
        return (first + " " + last).trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) return null;
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Money values may have at most two decimal places",
                    "INVALID_MONEY_SCALE");
        }
    }

    private record StockAllocation(MedicineBatches batch, int quantity) {
    }

    private record LineAmounts(BigDecimal taxRate,
                               BigDecimal taxableAmount,
                               BigDecimal tax,
                               BigDecimal total,
                               BigDecimal discount) {
    }

    private record PaymentTotals(BigDecimal paidTotal,
                                 BigDecimal cashTendered,
                                 BigDecimal changeDue,
                                 boolean pendingOnlinePayment,
                                 BigDecimal amountOwed) {
    }
}
