package com.example.pos.procurement.purchaseorders.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.medicine.repository.MedicineRepository;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderRequestDto;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderResponseDto;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.repository.SupplierRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PurchaseOrdersService {

    private final PurchaseOrdersRepository poRepo;
    private final SupplierRepository supplierRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final MedicineRepository medicineRepo;

    public PurchaseOrdersService(PurchaseOrdersRepository poRepo, SupplierRepository supplierRepo,
                                 BranchRepository branchRepo, UserRepository userRepo, MedicineRepository medicineRepo) {
        this.poRepo = poRepo;
        this.supplierRepo = supplierRepo;
        this.branchRepo = branchRepo;
        this.userRepo = userRepo;
        this.medicineRepo = medicineRepo;
    }

    public PurchaseOrders create(PurchaseOrderRequestDto dto) {
        Suppliers supplier = supplierRepo.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", dto.getSupplierId()));
        Branch branch = branchRepo.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        User orderedBy = userRepo.findById(dto.getOrderedById())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getOrderedById()));

        PurchaseOrders po = new PurchaseOrders();
        po.setSupplier(supplier);
        po.setBranch(branch);
        po.setOrderedBy(orderedBy);
        po.setOrderDate(LocalDateTime.now());
        po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        po.setStatus(PurchaseOrders.Status.ORDERED);

        poRepo.save(po);

        List<PurchaseOrderItems> items = new ArrayList<>();
        for (PurchaseOrderRequestDto.OrderItemDto itemDto : dto.getItems()) {
            Medicine medicine = medicineRepo.findById(itemDto.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine", itemDto.getMedicineId()));

            PurchaseOrderItems poi = new PurchaseOrderItems();
            poi.setPurchaseOrders(po);
            poi.setMedicine(medicine);
            poi.setQuantity(itemDto.getQuantity());
            poi.setBuyingPrice(itemDto.getBuyingPrice());
            poi.setDiscount(itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO);
            poi.setTax(itemDto.getTax() != null ? itemDto.getTax() : BigDecimal.ZERO);
            poi.setTotal(itemDto.getBuyingPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()))
                    .subtract(poi.getDiscount()).add(poi.getTax()));
            items.add(poi);
        }

        return po;
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrders> getByBranch(UUID branchId, Pageable pageable) {
        List<PurchaseOrders> list = poRepo.findByBranchId(branchId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrders> getBySupplier(UUID supplierId, Pageable pageable) {
        List<PurchaseOrders> list = poRepo.findBySupplierId(supplierId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public PurchaseOrders getById(UUID id) {
        return poRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
    }

    public PurchaseOrders approve(UUID id, UUID approvedById) {
        PurchaseOrders po = getById(id);
        User approver = userRepo.findById(approvedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", approvedById));
        po.setApprovedBy(approver);
        po.setStatus(PurchaseOrders.Status.IN_PROGRESS);
        return poRepo.save(po);
    }

    public PurchaseOrders markDelivered(UUID id) {
        PurchaseOrders po = getById(id);
        po.setStatus(PurchaseOrders.Status.DELIVERED);
        po.setDeliveryDate(LocalDateTime.now());
        return poRepo.save(po);
    }

    public PurchaseOrderResponseDto toDto(PurchaseOrders po) {
        PurchaseOrderResponseDto dto = PurchaseOrderResponseDto.from(po);
        if (po.getPurchaseOrderItems() != null) {
            List<PurchaseOrderResponseDto.OrderItemResponse> items = po.getPurchaseOrderItems().stream()
                    .map(i -> PurchaseOrderResponseDto.OrderItemResponse.builder()
                            .id(i.getId()).medicineId(i.getMedicine() != null ? i.getMedicine().getId() : null)
                            .medicineName(i.getMedicine() != null ? i.getMedicine().getBrandName() : null)
                            .quantity(i.getQuantity()).buyingPrice(i.getBuyingPrice())
                            .discount(i.getDiscount()).tax(i.getTax()).total(i.getTotal()).build())
                    .toList();
            dto.setItems(items);
        }
        return dto;
    }
}
