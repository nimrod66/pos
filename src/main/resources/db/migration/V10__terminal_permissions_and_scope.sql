INSERT INTO public.permissions (
    id, created_at, updated_at, version,
    permission_name, module_name, action_name, description
)
VALUES
    (gen_random_uuid(), now(), now(), 0, 'terminal.read', 'terminal', 'read',
     'View terminals, node health, and attached peripherals'),
    (gen_random_uuid(), now(), now(), 0, 'terminal.manage', 'terminal', 'manage',
     'Register, assign, approve, and configure terminals')
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO public.role_permissions (
    id, created_at, updated_at, version, permission_id, role_id
)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role ON role.role_name IN ('OWNER', 'BRANCH_MANAGER')
WHERE permission.permission_name IN ('terminal.read', 'terminal.manage')
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions existing
      WHERE existing.permission_id = permission.id
        AND existing.role_id = role.id
  );

ALTER TABLE public.terminal_registry
    DROP CONSTRAINT IF EXISTS uk_terminal_name;

ALTER TABLE public.terminal_registry
    ADD CONSTRAINT uk_terminal_branch_name UNIQUE (branch_id, name);

ALTER TABLE public.terminal_registry
    ADD CONSTRAINT fk_terminal_registry_branch
    FOREIGN KEY (branch_id) REFERENCES public.branch(id);
