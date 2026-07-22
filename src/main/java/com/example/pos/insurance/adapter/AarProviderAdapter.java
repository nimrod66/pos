package com.example.pos.insurance.adapter;

import com.example.pos.insurance.model.ClaimBatch;
import org.springframework.stereotype.Component;

@Component
public class AarProviderAdapter implements InsuranceProviderAdapter {

    @Override
    public String getProvider() {
        return "AAR";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ClaimSubmissionResult submitBatch(ClaimBatch batch) {
        return ClaimSubmissionResult.fail("AAR API integration not yet available — use file export");
    }

    @Override
    public ClaimSubmissionResult checkBatchStatus(String batchReference) {
        return ClaimSubmissionResult.fail("AAR API not available");
    }
}
