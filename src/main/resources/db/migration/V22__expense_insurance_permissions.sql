-- V22: Add expense and insurance permission grants
-- Creates permission records if missing, then grants to OWNER + BRANCH_MANAGER

INSERT INTO public.permissions (id, created_at, updated_at, version, permission_name)
SELECT gen_random_uuid(), now(), now(), 0, perm.name
FROM (VALUES ('expense.read'), ('expense.write'), ('insurance.read'), ('insurance.write')) AS perm(name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.permissions p WHERE p.permission_name = perm.name
);

INSERT INTO public.role_permissions (id, created_at, updated_at, version, permission_id, role_id)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role
  ON (role.role_name IN ('OWNER', 'BRANCH_MANAGER')
      AND permission.permission_name IN (
          'expense.read', 'expense.write', 'insurance.read', 'insurance.write'
      ))
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.permission_id = permission.id
      AND existing.role_id = role.id
);
