ALTER TABLE public.stock ADD COLUMN quantity_quarantined integer DEFAULT 0;
UPDATE public.stock SET quantity_quarantined = 0 WHERE quantity_quarantined IS NULL;
ALTER TABLE public.stock ALTER COLUMN quantity_quarantined SET NOT NULL;
ALTER TABLE public.stock ADD CONSTRAINT ck_stock_quarantined_nonnegative
    CHECK (quantity_quarantined >= 0);

ALTER TABLE public.sale_return_items ADD COLUMN refund_amount numeric(19,2);
ALTER TABLE public.sale_return_items ADD COLUMN disposition varchar(32);
UPDATE public.sale_return_items return_item
SET refund_amount = round(
        coalesce(sale_item.total, 0) / nullif(sale_item.quantity, 0)
            * coalesce(return_item.quantity, 0), 2),
    disposition = 'QUARANTINE'
FROM public.sales_items sale_item
WHERE sale_item.id = return_item.sale_items_id;
UPDATE public.sale_return_items
SET refund_amount = 0 WHERE refund_amount IS NULL;
UPDATE public.sale_return_items
SET disposition = 'QUARANTINE' WHERE disposition IS NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN sale_items_id SET NOT NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN medicine_batches_id SET NOT NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN sale_returns_id SET NOT NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN refund_amount SET NOT NULL;
ALTER TABLE public.sale_return_items ALTER COLUMN disposition SET NOT NULL;
ALTER TABLE public.sale_return_items ADD CONSTRAINT ck_sale_return_item_quantity_positive
    CHECK (quantity > 0);
ALTER TABLE public.sale_return_items ADD CONSTRAINT ck_sale_return_item_refund_nonnegative
    CHECK (refund_amount >= 0);
ALTER TABLE public.sale_return_items ADD CONSTRAINT ck_sale_return_item_disposition
    CHECK (disposition IN ('QUARANTINE'));

ALTER TABLE public.sale_returns ADD COLUMN client_return_id uuid;
ALTER TABLE public.sale_returns ADD COLUMN branch_id uuid;
ALTER TABLE public.sale_returns ADD COLUMN staff_shift_id uuid;
ALTER TABLE public.sale_returns ADD COLUMN refund_amount numeric(19,2);
ALTER TABLE public.sale_returns ADD COLUMN refund_method varchar(32);
ALTER TABLE public.sale_returns ADD COLUMN refund_reference varchar(120);

UPDATE public.sale_returns sale_return
SET client_return_id = sale_return.id,
    branch_id = sale.branch_id,
    staff_shift_id = sale.shift_id,
    user_id = coalesce(sale_return.user_id, sale.user_id),
    reason = coalesce(nullif(trim(sale_return.reason), ''), 'Legacy return'),
    return_date = coalesce(sale_return.return_date, sale_return.created_at),
    status = coalesce(nullif(trim(sale_return.status), ''), 'COMPLETED'),
    refund_method = 'LEGACY'
FROM public.sales sale
WHERE sale.id = sale_return.sales_id;
UPDATE public.sale_returns sale_return
SET refund_amount = coalesce((
        SELECT sum(return_item.refund_amount)
        FROM public.sale_return_items return_item
        WHERE return_item.sale_returns_id = sale_return.id), 0);

ALTER TABLE public.sale_returns ALTER COLUMN client_return_id SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN sales_id SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN staff_shift_id SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN reason SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN return_date SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN status SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN refund_amount SET NOT NULL;
ALTER TABLE public.sale_returns ALTER COLUMN refund_method SET NOT NULL;
ALTER TABLE public.sale_returns ADD CONSTRAINT fk_sale_return_branch
    FOREIGN KEY (branch_id) REFERENCES public.branch(id);
ALTER TABLE public.sale_returns ADD CONSTRAINT fk_sale_return_shift
    FOREIGN KEY (staff_shift_id) REFERENCES public.staff_shifts(id);
ALTER TABLE public.sale_returns ADD CONSTRAINT ck_sale_return_refund_nonnegative
    CHECK (refund_amount >= 0);
ALTER TABLE public.sale_returns ADD CONSTRAINT ck_sale_return_refund_method
    CHECK (refund_method IN ('CASH', 'MPESA_MANUAL', 'LEGACY'));
CREATE UNIQUE INDEX uk_sale_return_client_id
    ON public.sale_returns(client_return_id);
CREATE UNIQUE INDEX uk_sale_return_mpesa_reference
    ON public.sale_returns(lower(refund_reference))
    WHERE refund_method = 'MPESA_MANUAL';
CREATE INDEX ix_sale_return_branch_date
    ON public.sale_returns(branch_id, return_date DESC);
