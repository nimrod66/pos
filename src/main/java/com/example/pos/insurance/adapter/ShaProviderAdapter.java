package com.example.pos.insurance.adapter;

import com.example.pos.insurance.model.ClaimBatch;
import org.springframework.stereotype.Component;

@Component
public class ShaProviderAdapter implements InsuranceProviderAdapter {

    @Override
    public String getProvider() {
        return "SHA";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ClaimSubmissionResult submitBatch(ClaimBatch batch) {
        return ClaimSubmissionResult.fail("SHA API integration not yet available — use file export");
    }

    @Override
    public ClaimSubmissionResult checkBatchStatus(String batchReference) {
        return ClaimSubmissionResult.fail("SHA API not available");
    }
}
