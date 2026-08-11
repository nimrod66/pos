ALTER TABLE public.medicine ADD COLUMN pharmacy_id uuid;
ALTER TABLE public.medicine ADD COLUMN default_buying_price numeric(38,2);
ALTER TABLE public.medicine ADD COLUMN selling_price numeric(38,2);
ALTER TABLE public.medicine ADD COLUMN reorder_level integer;

UPDATE public.medicine medicine
SET pharmacy_id = coalesce(
        (SELECT branch.pharmacy_id
         FROM public.stock stock
         JOIN public.medicine_batches batch ON batch.id = stock.medicine_batches_id
         JOIN public.branch branch ON branch.id = stock.branch_id
         WHERE batch.medicine_id = medicine.id
         ORDER BY stock.created_at
         LIMIT 1),
        (SELECT pharmacy.id FROM public.pharmacy pharmacy ORDER BY pharmacy.created_at, pharmacy.id LIMIT 1)),
    default_buying_price = coalesce(
        (SELECT batch.buying_price FROM public.medicine_batches batch
         WHERE batch.medicine_id = medicine.id
         ORDER BY batch.created_at DESC LIMIT 1), 0),
    selling_price = coalesce(
        (SELECT batch.selling_price FROM public.medicine_batches batch
         WHERE batch.medicine_id = medicine.id
         ORDER BY batch.created_at DESC LIMIT 1), 0),
    reorder_level = coalesce(
        (SELECT max(stock.reorder_level)
         FROM public.stock stock
         JOIN public.medicine_batches batch ON batch.id = stock.medicine_batches_id
         WHERE batch.medicine_id = medicine.id), 0);

ALTER TABLE public.medicine ALTER COLUMN pharmacy_id SET NOT NULL;
ALTER TABLE public.medicine ALTER COLUMN default_buying_price SET NOT NULL;
ALTER TABLE public.medicine ALTER COLUMN selling_price SET NOT NULL;
ALTER TABLE public.medicine ALTER COLUMN reorder_level SET NOT NULL;
ALTER TABLE public.medicine ADD CONSTRAINT fk_medicine_pharmacy
    FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);
ALTER TABLE public.medicine ADD CONSTRAINT ck_medicine_prices_nonnegative
    CHECK (default_buying_price >= 0 AND selling_price >= 0);
ALTER TABLE public.medicine ADD CONSTRAINT ck_medicine_reorder_nonnegative
    CHECK (reorder_level >= 0);
ALTER TABLE public.medicine ADD CONSTRAINT uk_medicine_pharmacy_barcode
    UNIQUE (pharmacy_id, barcode);
CREATE UNIQUE INDEX uk_medicine_pharmacy_sku
    ON public.medicine(pharmacy_id, upper(sku)) WHERE sku IS NOT NULL;
CREATE INDEX ix_medicine_pharmacy_status ON public.medicine(pharmacy_id, status);

ALTER TABLE public.suppliers ADD COLUMN pharmacy_id uuid;
UPDATE public.suppliers supplier
SET pharmacy_id = coalesce(
        (SELECT branch.pharmacy_id
         FROM public.purchase_orders purchase_order
         JOIN public.branch branch ON branch.id = purchase_order.branch_id
         WHERE purchase_order.suppliers_id = supplier.id
         ORDER BY purchase_order.created_at
         LIMIT 1),
        (SELECT pharmacy.id FROM public.pharmacy pharmacy ORDER BY pharmacy.created_at, pharmacy.id LIMIT 1));
UPDATE public.suppliers SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE public.suppliers ALTER COLUMN pharmacy_id SET NOT NULL;
ALTER TABLE public.suppliers ALTER COLUMN supplier_name SET NOT NULL;
ALTER TABLE public.suppliers ALTER COLUMN status SET NOT NULL;
ALTER TABLE public.suppliers ADD CONSTRAINT fk_supplier_pharmacy
    FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);
CREATE UNIQUE INDEX uk_supplier_pharmacy_name
    ON public.suppliers(pharmacy_id, upper(supplier_name));
CREATE INDEX ix_supplier_pharmacy_status ON public.suppliers(pharmacy_id, status);

ALTER TABLE public.customer ADD COLUMN pharmacy_id uuid;
UPDATE public.customer customer
SET pharmacy_id = coalesce(
        (SELECT branch.pharmacy_id
         FROM public.sales sale
         JOIN public.branch branch ON branch.id = sale.branch_id
         WHERE sale.customer_id = customer.id
         ORDER BY sale.created_at
         LIMIT 1),
        (SELECT pharmacy.id FROM public.pharmacy pharmacy ORDER BY pharmacy.created_at, pharmacy.id LIMIT 1));
ALTER TABLE public.customer ALTER COLUMN pharmacy_id SET NOT NULL;
ALTER TABLE public.customer ADD CONSTRAINT fk_customer_pharmacy
    FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);
CREATE UNIQUE INDEX uk_customer_pharmacy_phone
    ON public.customer(pharmacy_id, phone_number) WHERE phone_number IS NOT NULL;
CREATE UNIQUE INDEX uk_customer_pharmacy_email
    ON public.customer(pharmacy_id, lower(email)) WHERE email IS NOT NULL;
CREATE INDEX ix_customer_pharmacy_name ON public.customer(pharmacy_id, last_name, first_name);

ALTER TABLE public.prescriptions ADD COLUMN branch_id uuid;
ALTER TABLE public.prescriptions ADD COLUMN approved_by uuid;
ALTER TABLE public.prescriptions ADD COLUMN approved_at timestamp(6) without time zone;
ALTER TABLE public.prescriptions ADD COLUMN dispensed_at timestamp(6) without time zone;
UPDATE public.prescriptions prescription
SET branch_id = coalesce(
        (SELECT sale.branch_id FROM public.sales sale
         WHERE sale.prescription_id = prescription.id
         ORDER BY sale.created_at LIMIT 1),
        (SELECT branch.id FROM public.branch branch ORDER BY branch.created_at, branch.id LIMIT 1)),
    approved_at = coalesce(prescription.created_at, current_timestamp),
    status = coalesce(prescription.status, 'ACTIVE');
UPDATE public.prescriptions prescription
SET approved_by = (
    SELECT app_user.id FROM public.users app_user
    WHERE app_user.branch_id = prescription.branch_id AND app_user.status = 'ACTIVE'
    ORDER BY app_user.created_at, app_user.id LIMIT 1)
WHERE prescription.approved_by IS NULL;
ALTER TABLE public.prescriptions ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.prescriptions ALTER COLUMN status SET NOT NULL;
ALTER TABLE public.prescriptions ADD CONSTRAINT fk_prescription_branch
    FOREIGN KEY (branch_id) REFERENCES public.branch(id);
ALTER TABLE public.prescriptions ADD CONSTRAINT fk_prescription_approver
    FOREIGN KEY (approved_by) REFERENCES public.users(id);
CREATE UNIQUE INDEX uk_prescription_branch_number
    ON public.prescriptions(branch_id, upper(prescription_number))
    WHERE prescription_number IS NOT NULL;
CREATE INDEX ix_prescription_branch_status ON public.prescriptions(branch_id, status);

ALTER TABLE public.prescription_items ALTER COLUMN prescription_id SET NOT NULL;
ALTER TABLE public.prescription_items ALTER COLUMN medicine_id SET NOT NULL;
ALTER TABLE public.prescription_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE public.prescription_items ADD CONSTRAINT ck_prescription_item_quantity_positive
    CHECK (quantity > 0);

INSERT INTO public.role_permissions(id, created_at, updated_at, version, permission_id, role_id)
SELECT gen_random_uuid(), current_timestamp, current_timestamp, 0, permission.id, role.id
FROM public.user_roles role
JOIN public.permissions permission ON permission.permission_name IN ('pos.sell', 'shift.open', 'shift.close')
WHERE role.role_name = 'PHARMACIST'
  AND NOT EXISTS (
      SELECT 1 FROM public.role_permissions existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id);

INSERT INTO public.role_permissions(id, created_at, updated_at, version, permission_id, role_id)
SELECT gen_random_uuid(), current_timestamp, current_timestamp, 0, permission.id, role.id
FROM public.user_roles role
JOIN public.permissions permission ON permission.permission_name = 'medicine.read'
WHERE role.role_name = 'CASHIER'
  AND NOT EXISTS (
      SELECT 1 FROM public.role_permissions existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id);
