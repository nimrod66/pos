package com.example.pos.sale.sales.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.customer.model.Customer;
import com.example.pos.customer.repository.CustomerRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.service.StockMovementsService;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.payment.repository.PaymentRepository;
import com.example.pos.sale.receipts.model.Receipts;
import com.example.pos.sale.receipts.repository.ReceiptsRepository;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.sales.dto.SaleRequestDto;
import com.example.pos.sale.sales.dto.SaleResponseDto;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.EventType;
import com.example.pos.sync.service.SyncService;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SaleService {

    private final SalesRepository salesRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final MedicineBatchesRepository batchesRepository;
    private final StockRepository stockRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptsRepository receiptsRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final TerminalConfig terminalConfig;
    private final SyncService syncService;
    private final StockMovementsService stockMovementsService;

    public SaleService(SalesRepository salesRepository, BranchRepository branchRepository,
                       UserRepository userRepository, CustomerRepository customerRepository,
                       MedicineBatchesRepository batchesRepository,
                       StockRepository stockRepository, PaymentRepository paymentRepository,
                       ReceiptsRepository receiptsRepository, IdempotencyKeyRepository idempotencyRepository,
                       TerminalConfig terminalConfig, SyncService syncService,
                       StockMovementsService stockMovementsService) {
        this.salesRepository = salesRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.batchesRepository = batchesRepository;
        this.stockRepository = stockRepository;
        this.paymentRepository = paymentRepository;
        this.receiptsRepository = receiptsRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.terminalConfig = terminalConfig;
        this.syncService = syncService;
        this.stockMovementsService = stockMovementsService;
    }

    public Sales createSale(SaleRequestDto dto) {
        if (dto.getIdempotencyKey() != null && idempotencyRepository.existsByIdempotencyKey(dto.getIdempotencyKey())) {
            throw new ConflictException("Duplicate transaction detected");
        }


        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        Sales sale = new Sales();
        sale.setBranch(branch);
        sale.setUser(user);
        sale.setInvoiceNumber(dto.getInvoiceNumber() != null ? dto.getInvoiceNumber() : generateInvoiceNumber());

        if (dto.getCustomerId() != null) {
            Customer customer = customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", dto.getCustomerId()));
            sale.setCustomer(customer);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<SaleItems> items = new ArrayList<>();

        for (SaleRequestDto.SaleItemDto itemDto : dto.getItems()) {
            MedicineBatches batch = batchesRepository.findById(itemDto.getMedicineBatchesId())
                    .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", itemDto.getMedicineBatchesId()));

            Stock stock = stockRepository.findByBranchIdAndMedicineBatchesId(dto.getBranchId(), itemDto.getMedicineBatchesId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock for branch " + dto.getBranchId() + " and batch " + itemDto.getMedicineBatchesId()));

            int available = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0;
            if (available < itemDto.getQuantity()) {
                throw new BadRequestException("Insufficient stock for batch " + batch.getBatchNumber()
                        + ". Available: " + available + ", requested: " + itemDto.getQuantity());
            }

            SaleItems si = new SaleItems();
            si.setSales(sale);
            si.setMedicineBatches(batch);
            si.setQuantity(itemDto.getQuantity());
            si.setPrice(itemDto.getPrice());
            si.setDiscount(itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO);
            si.setTax(itemDto.getTax() != null ? itemDto.getTax() : BigDecimal.ZERO);

            Medicine medicine = batch.getMedicine();
            Tax taxCategory = medicine != null ? medicine.getTax() : null;
            BigDecimal taxRate = taxCategory != null ? taxCategory.getTaxRate() : BigDecimal.ZERO;
            si.setTaxRate(taxRate);

            BigDecimal lineSubtotal = itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            si.setTaxableAmount(lineSubtotal.subtract(si.getDiscount()));

            BigDecimal lineTotal = lineSubtotal
                    .subtract(si.getDiscount())
                    .add(si.getTax());
            si.setTotal(lineTotal);

            subtotal = subtotal.add(itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            totalTax = totalTax.add(si.getTax());

            stock.setQuantityAvailable(available - itemDto.getQuantity());
            stockRepository.save(stock);

            items.add(si);
        }

        sale.setSubtotal(subtotal);
        sale.setTax(totalTax);
        sale.setTotal(subtotal.subtract(BigDecimal.ZERO).add(totalTax));
        sale.setSaleStatus(Sales.SaleStatus.DONE);
        sale.setTerminalId(terminalConfig.getTerminalId());
        sale.setSynced(!terminalConfig.isOffline());

        List<Payment> payments = new ArrayList<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (dto.getPayments() != null) {
            for (SaleRequestDto.PaymentItemDto pDto : dto.getPayments()) {
                Payment p = new Payment();
                p.setSales(sale);
                try {
                    p.setPaymentMethod(Payment.PaymentMethod.valueOf(pDto.getPaymentMethod().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid payment method: " + pDto.getPaymentMethod());
                }
                p.setAmount(pDto.getAmount());
                p.setCurrency(pDto.getCurrency() != null ? pDto.getCurrency() : "KES");
                p.setTransactionReference(pDto.getTransactionReference());
                p.setPaymentStatus("COMPLETED");
                p.setPaymentDate(LocalDateTime.now());
                totalPaid = totalPaid.add(pDto.getAmount());
                payments.add(p);
            }
        }

        sale.setPaymentStatus(totalPaid.compareTo(sale.getTotal()) >= 0
                ? Sales.PaymentStatus.PAID : Sales.PaymentStatus.NOT_PAID);

        if (dto.getIdempotencyKey() != null) {
            IdempotencyKey key = new IdempotencyKey();
            key.setIdempotencyKey(dto.getIdempotencyKey());
            key.setStatus(IdempotencyKey.Status.COMPLETED);
            key.setResourceType("SALE");
            key.setCreatedTime(LocalTime.now());
            idempotencyRepository.save(key);
            sale.setIdempotencyKey(key);
        }

        salesRepository.save(sale);
        items.forEach(i -> i.setSales(sale));
        payments.forEach(p -> p.setSales(sale));
        paymentRepository.saveAll(payments);

        for (SaleRequestDto.SaleItemDto itemDto : dto.getItems()) {
            stockMovementsService.recordDirect(dto.getBranchId(), itemDto.getMedicineBatchesId(),
                    dto.getUserId(), "SALE", itemDto.getQuantity(), "SALE", sale.getId());
            syncService.writeOutboxEvent(EventType.STOCK_DEDUCTED, "STOCK",
                    String.valueOf(dto.getBranchId()) + "-" + itemDto.getMedicineBatchesId(),
                    "{\"batchId\":" + itemDto.getMedicineBatchesId()
                            + ",\"quantity\":" + itemDto.getQuantity()
                            + ",\"branchId\":" + dto.getBranchId()
                            + ",\"saleUuid\":\"" + sale.getUuid() + "\"}");
        }

        Receipts receipt = new Receipts();
        receipt.setSales(sale);
        receipt.setReceiptNumber("RCT-" + sale.getInvoiceNumber());
        receipt.setPrintedDate(LocalDateTime.now());
        receiptsRepository.save(receipt);

        syncService.writeOutboxEvent(EventType.SALE_CREATED, "SALE", sale.getUuid(),
                "{\"invoiceNumber\":\"" + sale.getInvoiceNumber() + "\","
                        + "\"total\":" + sale.getTotal() + ","
                        + "\"terminalId\":\"" + terminalConfig.getTerminalId() + "\"}");

        return sale;
    }

    @Transactional(readOnly = true)
    public List<Sales> getSalesByBranch(Long branchId) {
        return salesRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public Sales getSaleById(Long id) {
        return salesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }

    public Sales cancelSale(Long id) {
        Sales sale = getSaleById(id);
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new BadRequestException("Sale is already cancelled");
        }
        if (sale.getPaymentStatus() == Sales.PaymentStatus.PAID) {
            throw new BadRequestException("Cannot cancel a fully paid sale; process as return instead");
        }
        sale.setSaleStatus(Sales.SaleStatus.CANCELLED);
        restockItems(sale);
        Sales savedCancel = salesRepository.save(sale);

        for (SaleItems si : sale.getSaleItems()) {
            stockMovementsService.recordDirect(sale.getBranch().getId(), si.getMedicineBatches().getId(),
                    sale.getUser().getId(), "RETURN", si.getQuantity(), "SALE_CANCEL", sale.getId());
            syncService.writeOutboxEvent(EventType.STOCK_RECEIVED, "STOCK",
                    String.valueOf(sale.getBranch().getId()) + "-" + si.getMedicineBatches().getId(),
                    "{\"batchId\":" + si.getMedicineBatches().getId()
                            + ",\"quantity\":" + si.getQuantity()
                            + ",\"branchId\":" + sale.getBranch().getId()
                            + ",\"saleUuid\":\"" + sale.getUuid() + "\"}");
        }

        syncService.writeOutboxEvent(EventType.SALE_CANCELLED, "SALE", savedCancel.getUuid(),
                "{\"invoiceNumber\":\"" + savedCancel.getInvoiceNumber() + "\"}");

        return savedCancel;
    }

    public Sales suspendSale(Long id) {
        Sales sale = getSaleById(id);
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new BadRequestException("Cannot suspend a cancelled sale");
        }
        if (sale.getSaleStatus() == Sales.SaleStatus.SUSPENDED) {
            throw new BadRequestException("Sale is already suspended");
        }
        if (sale.getPaymentStatus() == Sales.PaymentStatus.PAID) {
            throw new BadRequestException("Cannot suspend a fully paid sale");
        }
        sale.setSaleStatus(Sales.SaleStatus.SUSPENDED);
        restockItems(sale);
        Sales savedSuspend = salesRepository.save(sale);

        for (SaleItems si : sale.getSaleItems()) {
            stockMovementsService.recordDirect(sale.getBranch().getId(), si.getMedicineBatches().getId(),
                    sale.getUser().getId(), "RESERVATION_RELEASE", si.getQuantity(),
                    "SALE_SUSPEND", sale.getId());
            syncService.writeOutboxEvent(EventType.STOCK_RECEIVED, "STOCK",
                    String.valueOf(sale.getBranch().getId()) + "-" + si.getMedicineBatches().getId(),
                    "{\"batchId\":" + si.getMedicineBatches().getId()
                            + ",\"quantity\":" + si.getQuantity()
                            + ",\"branchId\":" + sale.getBranch().getId()
                            + ",\"saleUuid\":\"" + sale.getUuid() + "\"}");
        }

        syncService.writeOutboxEvent(EventType.SALE_SUSPENDED, "SALE", savedSuspend.getUuid(),
                "{\"invoiceNumber\":\"" + savedSuspend.getInvoiceNumber() + "\"}");

        return savedSuspend;
    }

    public Sales resumeSale(Long id) {
        Sales sale = getSaleById(id);
        if (sale.getSaleStatus() != Sales.SaleStatus.SUSPENDED) {
            throw new BadRequestException("Only suspended sales can be resumed");
        }
        for (SaleItems si : sale.getSaleItems()) {
            Stock stock = stockRepository.findByBranchIdAndMedicineBatchesId(
                    sale.getBranch().getId(), si.getMedicineBatches().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock for branch " + sale.getBranch().getId() + " and batch " + si.getMedicineBatches().getId()));
            int available = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0;
            if (available < si.getQuantity()) {
                throw new BadRequestException("Cannot resume: insufficient stock for " + si.getMedicineBatches().getBatchNumber());
            }
            stock.setQuantityAvailable(available - si.getQuantity());
            stockRepository.save(stock);
        }
        sale.setSaleStatus(Sales.SaleStatus.DONE);
        Sales saved = salesRepository.save(sale);

        for (SaleItems si : sale.getSaleItems()) {
            stockMovementsService.recordDirect(sale.getBranch().getId(), si.getMedicineBatches().getId(),
                    sale.getUser().getId(), "SALE", si.getQuantity(), "SALE_RESUME", sale.getId());
            syncService.writeOutboxEvent(EventType.STOCK_DEDUCTED, "STOCK",
                    String.valueOf(sale.getBranch().getId()) + "-" + si.getMedicineBatches().getId(),
                    "{\"batchId\":" + si.getMedicineBatches().getId()
                            + ",\"quantity\":" + si.getQuantity()
                            + ",\"branchId\":" + sale.getBranch().getId()
                            + ",\"saleUuid\":\"" + sale.getUuid() + "\"}");
        }

        syncService.writeOutboxEvent(EventType.SALE_RESUMED, "SALE", saved.getUuid(),
                "{\"invoiceNumber\":\"" + saved.getInvoiceNumber() + "\"}");

        return saved;
    }

    public Sales overrideItemPrice(Long saleId, Long itemId, BigDecimal newPrice, String reason) {
        Sales sale = getSaleById(saleId);
        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a cancelled sale");
        }
        SaleItems targetItem = null;
        for (SaleItems si : sale.getSaleItems()) {
            if (si.getId().equals(itemId)) {
                targetItem = si;
                break;
            }
        }
        if (targetItem == null) {
            throw new ResourceNotFoundException("SaleItem", itemId);
        }

        BigDecimal oldPrice = targetItem.getPrice();
        targetItem.setPrice(newPrice);
        BigDecimal lineTotal = newPrice.multiply(BigDecimal.valueOf(targetItem.getQuantity()))
                .subtract(targetItem.getDiscount())
                .add(targetItem.getTax());
        targetItem.setTotal(lineTotal);

        BigDecimal newSubtotal = BigDecimal.ZERO;
        for (SaleItems si : sale.getSaleItems()) {
            newSubtotal = newSubtotal.add(si.getPrice().multiply(BigDecimal.valueOf(si.getQuantity())));
        }
        sale.setSubtotal(newSubtotal);
        sale.setTotal(newSubtotal.add(sale.getTax()));
        return salesRepository.save(sale);
    }

    private void restockItems(Sales sale) {
        for (SaleItems si : sale.getSaleItems()) {
            Stock stock = stockRepository.findByBranchIdAndMedicineBatchesId(
                    sale.getBranch().getId(), si.getMedicineBatches().getId()).orElse(null);
            if (stock != null) {
                stock.setQuantityAvailable(
                        (stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0)
                                + si.getQuantity());
                stockRepository.save(stock);
            }
        }
    }

    @Transactional(readOnly = true)
    public Sales getLastSaleByUserAndBranch(Long userId, Long branchId) {
        return salesRepository.findTop1ByUserIdAndBranchIdOrderByCreatedAtDesc(userId, branchId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Sales> getSuspendedSales(Long branchId) {
        return salesRepository.findByBranchIdAndSaleStatus(branchId, Sales.SaleStatus.SUSPENDED);
    }

    @Transactional(readOnly = true)
    public List<Sales> getSalesByBranchAndDate(Long branchId, LocalDateTime start, LocalDateTime end) {
        return salesRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end);
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }

    public SaleResponseDto toResponseDto(Sales sale) {
        SaleResponseDto dto = SaleResponseDto.from(sale);

        List<SaleResponseDto.SaleItemResponse> itemResponses = new ArrayList<>();
        List<SaleResponseDto.PaymentResponse> paymentResponses = new ArrayList<>();

        if (sale.getSaleItems() != null) {
            for (SaleItems si : sale.getSaleItems()) {
                itemResponses.add(SaleResponseDto.SaleItemResponse.builder()
                        .id(si.getId())
                        .medicineBatchesId(si.getMedicineBatches() != null ? si.getMedicineBatches().getId() : null)
                        .batchNumber(si.getMedicineBatches() != null ? si.getMedicineBatches().getBatchNumber() : null)
                        .medicineName(si.getMedicineBatches() != null && si.getMedicineBatches().getMedicine() != null
                                ? si.getMedicineBatches().getMedicine().getBrandName() : null)
                        .quantity(si.getQuantity())
                        .price(si.getPrice())
                        .discount(si.getDiscount())
                        .taxRate(si.getTaxRate())
                        .taxableAmount(si.getTaxableAmount())
                        .tax(si.getTax())
                        .total(si.getTotal())
                        .build());
            }
        }

        if (sale.getPayment() != null) {
            for (Payment p : sale.getPayment()) {
                paymentResponses.add(SaleResponseDto.PaymentResponse.builder()
                        .id(p.getId())
                        .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .transactionReference(p.getTransactionReference())
                        .paymentStatus(p.getPaymentStatus())
                        .paymentDate(p.getPaymentDate())
                        .build());
            }
        }

        dto.setItems(itemResponses);
        dto.setPayments(paymentResponses);
        dto.setCustomerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null);
        dto.setCustomerName(sale.getCustomer() != null
                ? sale.getCustomer().getFirstName() + " " + (sale.getCustomer().getLastName() != null
                        ? sale.getCustomer().getLastName() : "")
                : null);
        return dto;
    }
}
