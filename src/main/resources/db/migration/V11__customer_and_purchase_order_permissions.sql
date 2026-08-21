INSERT INTO public.permissions (
    id, created_at, updated_at, version,
    permission_name, module_name, action_name, description
)
VALUES
    (gen_random_uuid(), now(), now(), 0, 'customer.read', 'customer', 'read',
     'View and search pharmacy customers'),
    (gen_random_uuid(), now(), now(), 0, 'customer.write', 'customer', 'write',
     'Create, update, and delete pharmacy customers'),
    (gen_random_uuid(), now(), now(), 0, 'purchase_order.read', 'purchase_order', 'read',
     'View pharmacy purchase orders'),
    (gen_random_uuid(), now(), now(), 0, 'purchase_order.write', 'purchase_order', 'write',
     'Create and update pharmacy purchase orders')
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO public.role_permissions (
    id, created_at, updated_at, version, permission_id, role_id
)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role
  ON (permission.permission_name = 'customer.read'
      AND role.role_name IN ('OWNER', 'BRANCH_MANAGER', 'PHARMACIST', 'CASHIER'))
  OR (permission.permission_name = 'customer.write'
      AND role.role_name IN ('OWNER', 'BRANCH_MANAGER', 'PHARMACIST', 'CASHIER'))
  OR (permission.permission_name = 'purchase_order.read'
      AND role.role_name IN ('OWNER', 'BRANCH_MANAGER', 'STORE_KEEPER'))
  OR (permission.permission_name = 'purchase_order.write'
      AND role.role_name IN ('OWNER', 'STORE_KEEPER'))
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.permission_id = permission.id
      AND existing.role_id = role.id
);
