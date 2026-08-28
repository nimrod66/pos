-- V24: Credit sales and customer account management
-- Adds customer balance/credit_limit, sale amount_owed, and customer_transactions ledger

-- 1. Add credit fields to customer table
ALTER TABLE public.customer ADD COLUMN balance numeric(38,2) DEFAULT 0;
ALTER TABLE public.customer ADD COLUMN credit_limit numeric(38,2);
ALTER TABLE public.customer ADD COLUMN account_status varchar(20) DEFAULT 'ACTIVE';

-- 2. Add amount_owed to sales table
ALTER TABLE public.sales ADD COLUMN amount_owed numeric(38,2) DEFAULT 0;
ALTER TABLE public.sales ADD COLUMN due_date timestamp;

-- 3. Add CREDIT to payment_method CHECK constraint
ALTER TABLE public.payments DROP CONSTRAINT payments_payment_method_check;
ALTER TABLE public.payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('MPESA_MANUAL', 'M_PESA', 'CASH', 'CARD', 'STRIPE', 'CREDIT'));

-- 4. Create customer_transactions ledger table
CREATE TABLE public.customer_transactions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    customer_id uuid NOT NULL,
    sale_id uuid,
    transaction_type varchar(30) NOT NULL,
    amount numeric(38,2) NOT NULL,
    running_balance numeric(38,2) NOT NULL,
    description varchar(500),
    reference varchar(100),
    recorded_by uuid
);

ALTER TABLE public.customer_transactions ADD CONSTRAINT pk_customer_transactions PRIMARY KEY (id);
ALTER TABLE public.customer_transactions ADD CONSTRAINT fk_ct_customer FOREIGN KEY (customer_id) REFERENCES public.customer(id);
ALTER TABLE public.customer_transactions ADD CONSTRAINT fk_ct_sale FOREIGN KEY (sale_id) REFERENCES public.sales(id);
ALTER TABLE public.customer_transactions ADD CONSTRAINT fk_ct_user FOREIGN KEY (recorded_by) REFERENCES public.users(id);
ALTER TABLE public.customer_transactions ADD CONSTRAINT ck_ct_amount_nonzero CHECK (amount != 0);

CREATE INDEX idx_ct_customer_date ON public.customer_transactions(customer_id, created_at);
CREATE INDEX idx_ct_sale ON public.customer_transactions(sale_id) WHERE sale_id IS NOT NULL;
