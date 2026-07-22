package com.example.pos.insurance.adapter;

import com.example.pos.insurance.model.ClaimBatch;
import org.springframework.stereotype.Component;

@Component
public class MockProviderAdapter implements InsuranceProviderAdapter {

    @Override
    public String getProvider() {
        return "MOCK";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ClaimSubmissionResult submitBatch(ClaimBatch batch) {
        return ClaimSubmissionResult.ok(batch.getBatchReference(), "ACKNOWLEDGED");
    }

    @Override
    public ClaimSubmissionResult checkBatchStatus(String batchReference) {
        return ClaimSubmissionResult.ok(batchReference, "SETTLED");
    }
}
