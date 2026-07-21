package com.example.pos.compliance.batch.service;

import com.example.pos.compliance.batch.model.Batch;
import com.example.pos.compliance.batch.model.BatchItem;
import com.example.pos.compliance.batch.model.BatchStatus;
import com.example.pos.compliance.batch.repository.BatchItemRepository;
import com.example.pos.compliance.batch.repository.BatchRepository;
import com.example.pos.compliance.gateway.vscu.VscuGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BatchProcessor {

    private final BatchRepository batchRepo;
    private final BatchItemRepository itemRepo;
    private final VscuGateway vscuGateway;

    public BatchProcessor(BatchRepository batchRepo, BatchItemRepository itemRepo, VscuGateway vscuGateway) {
        this.batchRepo = batchRepo;
        this.itemRepo = itemRepo;
        this.vscuGateway = vscuGateway;
    }

    public Batch createBatch() {
        return batchRepo.save(Batch.builder()
                .batchReference("BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build());
    }

    public BatchItem addToBatch(Long batchId, Long invoiceId, String invoiceNumber) {
        BatchItem item = BatchItem.builder()
                .batch(batchRepo.getReferenceById(batchId))
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .transmissionStatus("QUEUED")
                .build();
        return itemRepo.save(item);
    }

    public Batch sealBatch(Long batchId) {
        Batch batch = batchRepo.findById(batchId).orElseThrow();
        batch.setBatchStatus(BatchStatus.SEALED);
        List<BatchItem> items = itemRepo.findByBatchId(batchId);
        batch.setInvoiceCount(items.size());
        return batchRepo.save(batch);
    }

    public void submitBatch(Long batchId, List<String> payloads) {
        Batch batch = batchRepo.findById(batchId).orElseThrow();
        batch.setBatchStatus(BatchStatus.SUBMITTING);
        batch.setSubmittedAt(LocalDateTime.now());
        batchRepo.save(batch);

        var response = vscuGateway.submitBatch(batch, payloads);

        if (response.isSuccess()) {
            batch.setBatchStatus(BatchStatus.SUBMITTED);
        } else {
            batch.setBatchStatus(BatchStatus.FAILED);
        }
        batch.setCompletedAt(LocalDateTime.now());
        batchRepo.save(batch);
    }
}
