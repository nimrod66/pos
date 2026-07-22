package com.example.pos.insurance.adapter;

import com.example.pos.insurance.model.ClaimBatch;
import org.springframework.stereotype.Component;

@Component
public class FileExportAdapter implements InsuranceProviderAdapter {

    @Override
    public String getProvider() {
        return "FILE_EXPORT";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ClaimSubmissionResult submitBatch(ClaimBatch batch) {
        return ClaimSubmissionResult.ok(batch.getBatchReference(), "SUBMITTED");
    }

    @Override
    public ClaimSubmissionResult checkBatchStatus(String batchReference) {
        return ClaimSubmissionResult.ok(batchReference, "SUBMITTED");
    }
}
