-- Realignment of role permission bundles:
--  - BRANCH_MANAGER / STORE_KEEPER gain medicine.write (+ medicine.price.write)
--  - STORE_KEEPER / PHARMACY_TECHNICIAN gain dashboard.read
--  - PHARMACIST (and OWNER) gain prescription.approve so dispensing works
--    even when runtime demo seeding is disabled.

INSERT INTO public.role_permissions (
    id, created_at, updated_at, version, permission_id, role_id
)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role
  ON (role.role_name IN ('OWNER', 'PHARMACIST')
      AND permission.permission_name = 'prescription.approve')
  OR (role.role_name = 'BRANCH_MANAGER'
      AND permission.permission_name IN (
          'medicine.write',
          'medicine.price.write'
      ))
  OR (role.role_name = 'STORE_KEEPER'
      AND permission.permission_name IN (
          'medicine.price.write',
          'dashboard.read'
      ))
  OR (role.role_name = 'PHARMACY_TECHNICIAN'
      AND permission.permission_name = 'dashboard.read')
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.permission_id = permission.id
      AND existing.role_id = role.id
);
