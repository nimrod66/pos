package com.example.pos.pharmacy.regulatory.expiry.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.pharmacy.regulatory.expiry.dto.ExpiryLogRequestDto;
import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import com.example.pos.pharmacy.regulatory.expiry.repository.ExpiryLogsRepository;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExpiryLogsService {

    private final ExpiryLogsRepository repo;
    private final MedicineBatchesRepository batchRepo;
    private final UserRepository userRepo;

    public ExpiryLogsService(ExpiryLogsRepository repo, MedicineBatchesRepository batchRepo, UserRepository userRepo) {
        this.repo = repo;
        this.batchRepo = batchRepo;
        this.userRepo = userRepo;
    }

    public ExpiryLogs log(ExpiryLogRequestDto dto) {
        MedicineBatches batch = batchRepo.findById(dto.getMedicineBatchesId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", dto.getMedicineBatchesId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));
        ExpiryLogs log = new ExpiryLogs();
        log.setMedicineBatches(batch);
        log.setUser(user);
        log.setDisposalMethod(dto.getDisposalMethod());
        return repo.save(log);
    }

    @Transactional(readOnly = true)
    public List<ExpiryLogs> getByBatch(Long batchId) { return repo.findByMedicineBatchesId(batchId); }

    @Transactional(readOnly = true)
    public List<ExpiryLogs> getAll() { return repo.findAll(); }
}
