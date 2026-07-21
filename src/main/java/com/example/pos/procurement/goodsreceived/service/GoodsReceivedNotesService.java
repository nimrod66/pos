package com.example.pos.procurement.goodsreceived.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.goodsreceived.repository.GoodsReceivedNotesRepository;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.repository.PurchaseOrdersRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class GoodsReceivedNotesService {

    private final GoodsReceivedNotesRepository repo;
    private final PurchaseOrdersRepository poRepo;
    private final UserRepository userRepo;

    public GoodsReceivedNotesService(GoodsReceivedNotesRepository repo, PurchaseOrdersRepository poRepo, UserRepository userRepo) {
        this.repo = repo;
        this.poRepo = poRepo;
        this.userRepo = userRepo;
    }

    public GoodsReceivedNotes receive(GoodsReceivedRequestDto dto) {
        PurchaseOrders po = poRepo.findById(dto.getPurchaseOrdersId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", dto.getPurchaseOrdersId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        GoodsReceivedNotes grn = new GoodsReceivedNotes();
        grn.setPurchaseOrders(po);
        grn.setUser(user);
        grn.setReceivedAt(LocalDateTime.now());
        grn.setRemarks(dto.getRemarks());

        po.setStatus(PurchaseOrders.Status.DELIVERED);
        po.setDeliveryDate(LocalDateTime.now());
        poRepo.save(po);

        return repo.save(grn);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceivedNotes> getByPurchaseOrder(Long poId) {
        return repo.findByPurchaseOrdersId(poId);
    }
}
