ALTER TABLE staff_shifts DROP CONSTRAINT IF EXISTS staff_shifts_status_check;
ALTER TABLE staff_shifts ADD CONSTRAINT staff_shifts_status_check CHECK (status IN ('ACTIVE', 'CLOSED', 'CANCELLED', 'REVIEWED'));
