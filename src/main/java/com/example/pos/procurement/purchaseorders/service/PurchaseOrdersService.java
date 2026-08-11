package com.example.pos.procurement.purchaseorders.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderRequestDto;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderResponseDto;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PurchaseOrdersService {

    private final PurchaseOrdersRepository poRepo;
    private final SupplierRepository supplierRepo;
    private final MedicineRepository medicineRepo;
    private final AuthenticatedUserContext current;

    public PurchaseOrdersService(PurchaseOrdersRepository poRepo,
                                 SupplierRepository supplierRepo,
                                 MedicineRepository medicineRepo,
                                 AuthenticatedUserContext current) {
        this.poRepo = poRepo;
        this.supplierRepo = supplierRepo;
        this.medicineRepo = medicineRepo;
        this.current = current;
    }

    public PurchaseOrders create(PurchaseOrderRequestDto dto) {
        User orderedBy = current.user();
        Branch branch = orderedBy.getBranch();
        rejectSpoofedContext(dto, orderedBy, branch);
        Suppliers supplier = supplierRepo.findByIdAndPharmacyId(
                        dto.getSupplierId(), branch.getPharmacy().getId())
                .filter(value -> value.getStatus() == Suppliers.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active supplier", dto.getSupplierId()));
        if (dto.getExpectedDeliveryDate() != null
                && dto.getExpectedDeliveryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Expected delivery date must be in the future");
        }

        PurchaseOrders purchaseOrder = PurchaseOrders.builder()
                .supplier(supplier)
                .branch(branch)
                .orderedBy(orderedBy)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .status(PurchaseOrders.Status.ORDERED)
                .build();

        Set<UUID> medicineIds = new HashSet<>();
        for (PurchaseOrderRequestDto.OrderItemDto itemDto : dto.getItems()) {
            if (!medicineIds.add(itemDto.getMedicineId())) {
                throw new BadRequestException("A medicine may appear only once on a purchase order",
                        "DUPLICATE_PURCHASE_ORDER_MEDICINE");
            }
            Medicine medicine = medicineRepo.findByIdAndPharmacyId(
                            itemDto.getMedicineId(), branch.getPharmacy().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", itemDto.getMedicineId()));
            BigDecimal buyingPrice = money(itemDto.getBuyingPrice());
            BigDecimal discount = money(itemDto.getDiscount() == null ? BigDecimal.ZERO : itemDto.getDiscount());
            BigDecimal tax = money(itemDto.getTax() == null ? BigDecimal.ZERO : itemDto.getTax());
            BigDecimal gross = buyingPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            if (discount.compareTo(gross) > 0) {
                throw new BadRequestException("Line discount cannot exceed its gross value");
            }
            PurchaseOrderItems item = PurchaseOrderItems.builder()
                    .purchaseOrders(purchaseOrder)
                    .medicine(medicine)
                    .quantity(itemDto.getQuantity())
                    .buyingPrice(buyingPrice)
                    .discount(discount)
                    .tax(tax)
                    .total(money(gross.subtract(discount).add(tax)))
                    .build();
            purchaseOrder.getPurchaseOrderItems().add(item);
        }
        return poRepo.saveAndFlush(purchaseOrder);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrders> getByBranch(UUID branchId, Pageable pageable) {
        Branch branch = current.branch();
        if (branchId != null) current.requireBranch(branchId);
        return poRepo.findByBranchId(branch.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrders> getBySupplier(UUID supplierId, Pageable pageable) {
        Branch branch = current.branch();
        supplierRepo.findByIdAndPharmacyId(supplierId, branch.getPharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        return poRepo.findBySupplierIdAndBranchId(supplierId, branch.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public PurchaseOrders getById(UUID id) {
        return poRepo.findDetailedByIdAndBranchId(id, current.branch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
    }

    public PurchaseOrders approve(UUID id, UUID approvedById) {
        User approver = current.user();
        if (approvedById != null && !approver.getId().equals(approvedById)) {
            throw new ForbiddenException("Approver must match the authenticated user");
        }
        PurchaseOrders purchaseOrder = getById(id);
        if (purchaseOrder.getStatus() != PurchaseOrders.Status.ORDERED) {
            throw new ConflictException("Only an ordered purchase order can be approved",
                    "PURCHASE_ORDER_NOT_APPROVABLE");
        }
        purchaseOrder.setApprovedBy(approver);
        purchaseOrder.setStatus(PurchaseOrders.Status.IN_PROGRESS);
        return poRepo.save(purchaseOrder);
    }

    public PurchaseOrders markDelivered(UUID id) {
        getById(id);
        throw new ConflictException("Purchase orders are delivered by completing their GRNs",
                "GRN_REQUIRED");
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto toDto(PurchaseOrders purchaseOrder) {
        PurchaseOrders detailed = getById(purchaseOrder.getId());
        PurchaseOrderResponseDto dto = PurchaseOrderResponseDto.from(detailed);
        List<PurchaseOrderResponseDto.OrderItemResponse> items = detailed.getPurchaseOrderItems().stream()
                .map(item -> PurchaseOrderResponseDto.OrderItemResponse.builder()
                        .id(item.getId())
                        .medicineId(item.getMedicine().getId())
                        .medicineName(item.getMedicine().getBrandName())
                        .quantity(item.getQuantity())
                        .buyingPrice(item.getBuyingPrice())
                        .discount(item.getDiscount())
                        .tax(item.getTax())
                        .total(item.getTotal())
                        .build())
                .toList();
        dto.setItems(items);
        return dto;
    }

    private void rejectSpoofedContext(PurchaseOrderRequestDto dto, User user, Branch branch) {
        if (!branch.getId().equals(dto.getBranchId()) || !user.getId().equals(dto.getOrderedById())) {
            throw new ForbiddenException("Branch and ordering user must match the active session");
        }
    }

    private BigDecimal money(BigDecimal value) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Money values may have at most two decimal places",
                    "INVALID_MONEY_SCALE");
        }
    }
}
