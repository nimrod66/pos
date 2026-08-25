-- Terminal pairing and per-person assignment.
ALTER TABLE public.terminal_registry
    ADD COLUMN pairing_code varchar(8),
    ADD COLUMN pairing_expires_at timestamp,
    ADD COLUMN assigned_user_id uuid;

CREATE INDEX ix_terminal_pairing_code ON public.terminal_registry (pairing_code)
    WHERE pairing_code IS NOT NULL;
