ALTER TABLE public.audit_logs ADD COLUMN pharmacy_id uuid;
ALTER TABLE public.audit_logs ADD COLUMN branch_id uuid;

UPDATE public.audit_logs audit
SET branch_id = coalesce(
        (SELECT app_user.branch_id FROM public.users app_user WHERE app_user.id = audit.user_id),
        (SELECT branch.id FROM public.branch branch ORDER BY branch.created_at, branch.id LIMIT 1));
UPDATE public.audit_logs audit
SET pharmacy_id = coalesce(
        (SELECT branch.pharmacy_id FROM public.branch branch WHERE branch.id = audit.branch_id),
        (SELECT pharmacy.id FROM public.pharmacy pharmacy ORDER BY pharmacy.created_at, pharmacy.id LIMIT 1));

ALTER TABLE public.audit_logs ALTER COLUMN pharmacy_id SET NOT NULL;
ALTER TABLE public.audit_logs ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.audit_logs ADD CONSTRAINT fk_audit_pharmacy
    FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);
ALTER TABLE public.audit_logs ADD CONSTRAINT fk_audit_branch
    FOREIGN KEY (branch_id) REFERENCES public.branch(id);
CREATE INDEX ix_audit_pharmacy_created_at ON public.audit_logs(pharmacy_id, created_at DESC);
CREATE INDEX ix_audit_branch_created_at ON public.audit_logs(branch_id, created_at DESC);
CREATE INDEX ix_audit_record ON public.audit_logs(pharmacy_id, table_name, record_id);

CREATE UNIQUE INDEX uk_branch_pharmacy_code
    ON public.branch(pharmacy_id, upper(branch_code));
CREATE UNIQUE INDEX uk_branch_pharmacy_email
    ON public.branch(pharmacy_id, lower(email)) WHERE email IS NOT NULL;

ALTER TABLE public.purchase_orders ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.purchase_orders ALTER COLUMN suppliers_id SET NOT NULL;
ALTER TABLE public.purchase_orders ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.purchase_orders ALTER COLUMN order_date SET NOT NULL;
ALTER TABLE public.purchase_orders ALTER COLUMN status SET NOT NULL;
CREATE INDEX ix_purchase_order_branch_status
    ON public.purchase_orders(branch_id, status, created_at DESC);

ALTER TABLE public.purchase_order_items ALTER COLUMN purchase_orders_id SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN medicine_id SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN buying_price SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN discount SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN tax SET NOT NULL;
ALTER TABLE public.purchase_order_items ALTER COLUMN total SET NOT NULL;
ALTER TABLE public.purchase_order_items ADD CONSTRAINT ck_purchase_order_item_quantity_positive
    CHECK (quantity > 0);
ALTER TABLE public.purchase_order_items ADD CONSTRAINT ck_purchase_order_item_money_nonnegative
    CHECK (buying_price > 0 AND discount >= 0 AND tax >= 0 AND total >= 0);
