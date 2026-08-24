ALTER TABLE public.sales
    ALTER COLUMN completed_at DROP NOT NULL;

ALTER TABLE public.payments
    ADD COLUMN merchant_request_id varchar(100),
    ADD COLUMN checkout_request_id varchar(100);

CREATE UNIQUE INDEX uk_payments_mpesa_merchant_request
    ON public.payments(merchant_request_id)
    WHERE merchant_request_id IS NOT NULL;

CREATE UNIQUE INDEX uk_payments_mpesa_checkout_request
    ON public.payments(checkout_request_id)
    WHERE checkout_request_id IS NOT NULL;

INSERT INTO public.permissions (
    id, created_at, updated_at, version,
    permission_name, module_name, action_name, description
)
VALUES (
    gen_random_uuid(), now(), now(), 0,
    'prescription.read', 'prescription', 'read',
    'View branch prescriptions and prescription medicines'
)
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO public.user_roles (
    id, created_at, updated_at, version, role_name, description
)
VALUES (
    gen_random_uuid(), now(), now(), 0,
    'PHARMACY_TECHNICIAN',
    'Assist with sales, customers, stock receiving, and prescriptions under supervision'
)
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO public.role_permissions (
    id, created_at, updated_at, version, permission_id, role_id
)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role
  ON (role.role_name IN ('OWNER', 'BRANCH_MANAGER', 'PHARMACIST')
      AND permission.permission_name = 'prescription.read')
  OR (role.role_name = 'PHARMACY_TECHNICIAN'
      AND permission.permission_name IN (
          'pos.sell',
          'sale.read',
          'sale.receipt.reprint',
          'medicine.read',
          'inventory.read',
          'inventory.receive',
          'supplier.read',
          'customer.read',
          'customer.write',
          'purchase_order.read',
          'shift.open',
          'shift.close',
          'prescription.read'
      ))
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.permission_id = permission.id
      AND existing.role_id = role.id
);

INSERT INTO public.user_branch_role (
    id, created_at, updated_at, version,
    assigned_at, assigned_by, branch_id, user_id, user_roles_id
)
SELECT gen_random_uuid(), now(), now(), 0, now(), assignment.assigned_by,
       assignment.branch_id, assignment.user_id, technician_role.id
FROM public.user_branch_role assignment
JOIN public.users user_account ON user_account.id = assignment.user_id
CROSS JOIN public.user_roles technician_role
WHERE lower(user_account.email) = 'technician@demo.com'
  AND technician_role.role_name = 'PHARMACY_TECHNICIAN'
  AND NOT EXISTS (
      SELECT 1
      FROM public.user_branch_role existing
      WHERE existing.user_id = assignment.user_id
        AND existing.branch_id = assignment.branch_id
        AND existing.user_roles_id = technician_role.id
  )
LIMIT 1;

DELETE FROM public.user_branch_role assignment
USING public.users user_account, public.user_roles assigned_role
WHERE assignment.user_id = user_account.id
  AND assignment.user_roles_id = assigned_role.id
  AND lower(user_account.email) = 'technician@demo.com'
  AND assigned_role.role_name IN ('CASHIER', 'STORE_KEEPER');
