-- Audit before/after snapshots exceed varchar(255); widen to TEXT.
ALTER TABLE public.audit_logs ALTER COLUMN old_value TYPE text;
ALTER TABLE public.audit_logs ALTER COLUMN new_value TYPE text;
