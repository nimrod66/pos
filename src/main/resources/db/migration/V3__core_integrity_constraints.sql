ALTER TABLE public.goods_received_notes ADD COLUMN branch_id uuid;

UPDATE public.goods_received_notes grn
SET branch_id = po.branch_id
FROM public.purchase_orders po
WHERE grn.purchase_orders_id = po.id
  AND grn.branch_id IS NULL;

UPDATE public.goods_received_notes grn
SET branch_id = u.branch_id
FROM public.users u
WHERE grn.received_by_user_id = u.id
  AND grn.branch_id IS NULL;

ALTER TABLE public.goods_received_notes ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.goods_received_notes
    ADD CONSTRAINT fk_grn_branch FOREIGN KEY (branch_id) REFERENCES public.branch(id);
CREATE INDEX ix_grn_branch ON public.goods_received_notes(branch_id);
CREATE UNIQUE INDEX uk_grn_branch_idempotency
    ON public.goods_received_notes(branch_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE public.idempotency ADD COLUMN pharmacy_id uuid;

UPDATE public.idempotency ik
SET pharmacy_id = b.pharmacy_id
FROM public.sales s
JOIN public.branch b ON b.id = s.branch_id
WHERE s.idempotency_id = ik.id
  AND ik.pharmacy_id IS NULL;

UPDATE public.idempotency ik
SET pharmacy_id = b.pharmacy_id
FROM public.goods_received_notes grn
JOIN public.branch b ON b.id = grn.branch_id
WHERE grn.idempotency_key = ik.idempotency_key
  AND ik.pharmacy_id IS NULL;

UPDATE public.idempotency
SET pharmacy_id = (SELECT id FROM public.pharmacy ORDER BY created_at LIMIT 1)
WHERE pharmacy_id IS NULL
  AND (SELECT count(*) FROM public.pharmacy) = 1;

ALTER TABLE public.idempotency ALTER COLUMN pharmacy_id SET NOT NULL;
ALTER TABLE public.idempotency ALTER COLUMN idempotency_key TYPE varchar(64);
ALTER TABLE public.idempotency ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE public.idempotency
    ADD CONSTRAINT fk_idempotency_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);
ALTER TABLE public.idempotency
    ADD CONSTRAINT uk_idempotency_pharmacy_key UNIQUE (pharmacy_id, idempotency_key);
CREATE INDEX ix_idempotency_pharmacy ON public.idempotency(pharmacy_id);

ALTER TABLE public.stock ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.stock ALTER COLUMN medicine_batches_id SET NOT NULL;
ALTER TABLE public.stock
    ADD CONSTRAINT uk_stock_branch_batch UNIQUE (branch_id, medicine_batches_id);
ALTER TABLE public.stock
    ADD CONSTRAINT ck_stock_quantities_nonnegative CHECK (
        coalesce(quantity_available, 0) >= 0
        AND coalesce(reserved_quantity, 0) >= 0
    );

ALTER TABLE public.medicine_batches ALTER COLUMN medicine_id SET NOT NULL;
ALTER TABLE public.medicine_batches ALTER COLUMN batch_number TYPE varchar(100);
ALTER TABLE public.medicine_batches ALTER COLUMN batch_number SET NOT NULL;
ALTER TABLE public.medicine_batches
    ADD CONSTRAINT uk_batch_medicine_number UNIQUE (medicine_id, batch_number);
ALTER TABLE public.medicine_batches
    ADD CONSTRAINT ck_batch_initial_quantity_nonnegative CHECK (coalesce(initial_quantity, 0) >= 0);

ALTER TABLE public.users ALTER COLUMN email SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN password_hash SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN status SET NOT NULL;
CREATE UNIQUE INDEX uk_users_email_normalized ON public.users(lower(email));

ALTER TABLE public.user_branch_role
    ADD CONSTRAINT uk_user_branch_role UNIQUE (user_id, branch_id, user_roles_id);

CREATE UNIQUE INDEX uk_staff_shift_active_user
    ON public.staff_shifts(user_id)
    WHERE status = 'ACTIVE';
