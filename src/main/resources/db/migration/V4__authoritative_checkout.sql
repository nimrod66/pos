ALTER TABLE public.sales ADD COLUMN client_sale_id uuid;
ALTER TABLE public.sales ADD COLUMN shift_id uuid;
ALTER TABLE public.sales ADD COLUMN prescription_id uuid;
ALTER TABLE public.sales ADD COLUMN discount_total numeric(38,2);
ALTER TABLE public.sales ADD COLUMN paid_total numeric(38,2);
ALTER TABLE public.sales ADD COLUMN cash_tendered numeric(38,2);
ALTER TABLE public.sales ADD COLUMN change_due numeric(38,2);
ALTER TABLE public.sales ADD COLUMN currency varchar(3);
ALTER TABLE public.sales ADD COLUMN note varchar(500);
ALTER TABLE public.sales ADD COLUMN completed_at timestamp(6) without time zone;

UPDATE public.sales
SET client_sale_id = id,
    discount_total = 0,
    paid_total = coalesce(total, 0),
    cash_tendered = coalesce(total, 0),
    change_due = 0,
    currency = 'KES',
    completed_at = created_at
WHERE client_sale_id IS NULL;

UPDATE public.sales sale
SET shift_id = shift.id
FROM public.staff_shifts shift
WHERE shift.user_id = sale.user_id
  AND shift.branch_id = sale.branch_id
  AND shift.shift_start_time <= sale.created_at
  AND (shift.shift_end_time IS NULL OR shift.shift_end_time >= sale.created_at)
  AND sale.shift_id IS NULL;

ALTER TABLE public.sales ALTER COLUMN client_sale_id SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN shift_id SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN discount_total SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN paid_total SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN cash_tendered SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN change_due SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN currency SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN completed_at SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN invoice_number TYPE varchar(64);
ALTER TABLE public.sales ALTER COLUMN invoice_number SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE public.sales ALTER COLUMN sale_status SET NOT NULL;
ALTER TABLE public.sales DROP CONSTRAINT sales_sale_status_check;
ALTER TABLE public.sales ADD CONSTRAINT sales_sale_status_check
    CHECK (sale_status IN ('COMPLETED', 'DONE', 'CANCELLED', 'SUSPENDED'));
ALTER TABLE public.sales
    ADD CONSTRAINT fk_sales_shift FOREIGN KEY (shift_id) REFERENCES public.staff_shifts(id);
ALTER TABLE public.sales
    ADD CONSTRAINT fk_sales_prescription FOREIGN KEY (prescription_id) REFERENCES public.prescriptions(id);
ALTER TABLE public.sales ADD CONSTRAINT uk_sales_client_sale UNIQUE (client_sale_id);
ALTER TABLE public.sales ADD CONSTRAINT uk_sales_invoice_number UNIQUE (invoice_number);
CREATE INDEX ix_sales_branch_created_at ON public.sales(branch_id, created_at DESC);
CREATE INDEX ix_sales_shift ON public.sales(shift_id);
CREATE INDEX ix_sales_prescription ON public.sales(prescription_id);

ALTER TABLE public.sales_items ADD COLUMN client_line_id uuid;
UPDATE public.sales_items SET client_line_id = id WHERE client_line_id IS NULL;
ALTER TABLE public.sales_items ALTER COLUMN client_line_id SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN sales_id SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN medicine_batches_id SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN price SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN discount SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN tax SET NOT NULL;
ALTER TABLE public.sales_items ALTER COLUMN total SET NOT NULL;
CREATE INDEX ix_sales_items_sale ON public.sales_items(sales_id);

ALTER TABLE public.payments ALTER COLUMN sales_id SET NOT NULL;
ALTER TABLE public.payments ALTER COLUMN payment_method SET NOT NULL;
ALTER TABLE public.payments ALTER COLUMN amount SET NOT NULL;
ALTER TABLE public.payments ALTER COLUMN currency SET NOT NULL;
ALTER TABLE public.payments ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE public.payments DROP CONSTRAINT payments_payment_method_check;
ALTER TABLE public.payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('MPESA_MANUAL', 'M_PESA', 'CASH', 'CARD', 'STRIPE'));
ALTER TABLE public.payments ADD CONSTRAINT ck_payment_amount_positive CHECK (amount > 0);
CREATE UNIQUE INDEX uk_payments_mpesa_reference
    ON public.payments(upper(transaction_reference))
    WHERE payment_method = 'MPESA_MANUAL' AND transaction_reference IS NOT NULL;

ALTER TABLE public.receipts ALTER COLUMN sales_id SET NOT NULL;
ALTER TABLE public.receipts ALTER COLUMN receipt_number SET NOT NULL;
ALTER TABLE public.receipts ADD CONSTRAINT uk_receipt_sale UNIQUE (sales_id);
ALTER TABLE public.receipts ADD CONSTRAINT uk_receipt_number UNIQUE (receipt_number);
