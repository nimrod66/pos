ALTER TABLE public.staff_shifts ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE public.staff_shifts ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.staff_shifts ALTER COLUMN shift_start_time SET NOT NULL;
ALTER TABLE public.staff_shifts ALTER COLUMN status SET NOT NULL;

CREATE INDEX ix_staff_shift_branch_started
    ON public.staff_shifts(branch_id, shift_start_time DESC);

ALTER TABLE public.cash_drawers ALTER COLUMN staff_shifts_id SET NOT NULL;
ALTER TABLE public.cash_drawers ALTER COLUMN opening_balance SET NOT NULL;
ALTER TABLE public.cash_drawers ALTER COLUMN expected_closing_balance SET NOT NULL;
ALTER TABLE public.cash_drawers ALTER COLUMN opening_time SET NOT NULL;
ALTER TABLE public.cash_drawers ALTER COLUMN status SET NOT NULL;
ALTER TABLE public.cash_drawers ADD CONSTRAINT ck_cash_drawer_balances_nonnegative
    CHECK (opening_balance >= 0 AND expected_closing_balance >= 0
        AND (actual_closing_balance IS NULL OR actual_closing_balance >= 0));
CREATE UNIQUE INDEX uk_cash_drawer_open_shift
    ON public.cash_drawers(staff_shifts_id) WHERE status = 'OPEN';
