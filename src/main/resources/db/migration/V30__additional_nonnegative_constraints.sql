-- V30: Additional non-negative constraints for authoritative quantities
-- Ensures the database rejects invalid state even if application logic is bypassed.

-- GRN line quantities must be positive (you receive at least 1 unit)
ALTER TABLE public.grn_lines
    ADD CONSTRAINT ck_grn_line_quantity_positive
    CHECK (quantity > 0);

-- Dispensed quantities must be positive
ALTER TABLE public.dispensed_items
    ADD CONSTRAINT ck_dispensed_quantity_positive
    CHECK (dispensed_quantity > 0);

-- Stock count quantities must be non-negative
ALTER TABLE public.stock_count_items
    ADD CONSTRAINT ck_stock_count_quantity_nonnegative
    CHECK (counted_quantity >= 0);

-- Stock transfer quantities must be positive
ALTER TABLE public.stock_transfer_items
    ADD CONSTRAINT ck_transfer_quantity_positive
    CHECK (quantity > 0);

-- Expense amounts must be positive
ALTER TABLE public.expenses
    ADD CONSTRAINT ck_expense_amount_positive
    CHECK (amount > 0);

-- Controlled drug quantities must be positive
ALTER TABLE public.controlled_drugs
    ADD CONSTRAINT ck_controlled_drug_quantity_positive
    CHECK (quantity_dispensed > 0);

-- Cash transaction amounts must be non-zero (positive for in, negative for out)
ALTER TABLE public.cash_transactions
    ADD CONSTRAINT ck_cash_transaction_amount_nonzero
    CHECK (amount <> 0);

-- Customer transaction amounts must be non-zero
-- (already has ck_ct_amount_nonzero, but let's ensure it covers the constraint)
-- Skip if already exists.

-- Insurance claim amounts must be non-negative
ALTER TABLE public.insurance_claims
    ADD CONSTRAINT ck_insurance_claim_amount_nonnegative
    CHECK (claim_amount >= 0);

-- Insurance claim co-pay must be non-negative
ALTER TABLE public.insurance_claims
    ADD CONSTRAINT ck_insurance_copay_nonnegative
    CHECK (co_pay_amount >= 0);
