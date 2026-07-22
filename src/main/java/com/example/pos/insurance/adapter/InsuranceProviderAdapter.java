package com.example.pos.insurance.adapter;

import com.example.pos.insurance.model.ClaimBatch;

public interface InsuranceProviderAdapter {

    String getProvider();

    boolean isAvailable();

    ClaimSubmissionResult submitBatch(ClaimBatch batch);

    ClaimSubmissionResult checkBatchStatus(String batchReference);

    record ClaimSubmissionResult(boolean success, String reference, String status, String message) {
        public static ClaimSubmissionResult ok(String ref, String status) {
            return new ClaimSubmissionResult(true, ref, status, null);
        }
        public static ClaimSubmissionResult fail(String message) {
            return new ClaimSubmissionResult(false, null, null, message);
        }
    }
}
