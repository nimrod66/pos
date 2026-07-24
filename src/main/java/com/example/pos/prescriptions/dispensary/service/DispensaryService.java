package com.example.pos.prescriptions.dispensary.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.prescriptions.dispensary.dto.DispensaryRequestDto;
import com.example.pos.prescriptions.dispensary.model.Dispensary;
import com.example.pos.prescriptions.dispensary.repository.DispensaryRepository;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.prescriptions.prescriptionitems.repository.PrescriptionItemsRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DispensaryService {

    private final DispensaryRepository repo;
    private final MedicineBatchesRepository batchRepo;
    private final UserRepository userRepo;
    private final PrescriptionItemsRepository prescriptionItemsRepo;

    public DispensaryService(DispensaryRepository repo, MedicineBatchesRepository batchRepo,
                             UserRepository userRepo, PrescriptionItemsRepository prescriptionItemsRepo) {
        this.repo = repo;
        this.batchRepo = batchRepo;
        this.userRepo = userRepo;
        this.prescriptionItemsRepo = prescriptionItemsRepo;
    }

    public Dispensary dispense(DispensaryRequestDto dto) {
        MedicineBatches batch = batchRepo.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));
        PrescriptionItems pi = prescriptionItemsRepo.findById(dto.getPrescriptionItemsId())
                .orElseThrow(() -> new ResourceNotFoundException("PrescriptionItem", dto.getPrescriptionItemsId()));

        Dispensary dispensary = new Dispensary();
        dispensary.setMedicineBatches(batch);
        dispensary.setUser(user);
        dispensary.setPrescriptionItems(pi);
        dispensary.setDispensedQuantity(dto.getDispensedQuantity());
        dispensary.setDispensingDate(LocalDateTime.now());
        return repo.save(dispensary);
    }

    @Transactional(readOnly = true)
    public Page<Dispensary> getByBatch(Long batchId, Pageable pageable) {
        List<Dispensary> list = repo.findByMedicineBatchesId(batchId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Page<Dispensary> getByUser(Long userId, Pageable pageable) {
        List<Dispensary> list = repo.findByUserId(userId);
        return new PageImpl<>(list, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public Dispensary getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispensary", id));
    }
}
