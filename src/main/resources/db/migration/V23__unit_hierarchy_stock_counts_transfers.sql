-- V23: Unit hierarchy, stock counts, stock transfers, sale item unit tracking
-- Feature 1: Unit hierarchy (parent/conversion)
-- Feature 2: Stock counts & reconciliation
-- Feature 3: Inter-branch stock transfers

-- ═══════════════════════════════════════════════════════════════
-- FEATURE 1: Unit hierarchy
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE unit_of_measure
    ADD COLUMN parent_unit_id UUID REFERENCES unit_of_measure(id),
    ADD COLUMN conversion_factor INTEGER DEFAULT 1;

-- Seed conversion factors: Box(10)→Strip(10)→Tablet(1)
UPDATE unit_of_measure SET conversion_factor = 10, parent_unit_id = (
    SELECT id FROM unit_of_measure WHERE unit_name = 'Strip'
) WHERE unit_name = 'Box';

UPDATE unit_of_measure SET conversion_factor = 10, parent_unit_id = (
    SELECT id FROM unit_of_measure WHERE unit_name = 'Tablet'
) WHERE unit_name = 'Strip';

UPDATE unit_of_measure SET conversion_factor = 100, parent_unit_id = (
    SELECT id FROM unit_of_measure WHERE unit_name = 'Tablet'
) WHERE unit_name = 'Box' AND parent_unit_id IS NULL;

-- ═══════════════════════════════════════════════════════════════
-- FEATURE 2: Stock counts & reconciliation
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE stock_counts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    branch_id UUID NOT NULL REFERENCES branch(id),
    count_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    counted_by_id UUID NOT NULL REFERENCES users(id),
    reviewed_by_id UUID REFERENCES users(id),
    remarks TEXT,
    CONSTRAINT uk_stock_count_branch_date UNIQUE (branch_id, count_date)
);

CREATE TABLE stock_count_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    stock_count_id UUID NOT NULL REFERENCES stock_counts(id) ON DELETE CASCADE,
    medicine_batches_id UUID NOT NULL REFERENCES medicine_batches(id),
    system_quantity INTEGER NOT NULL,
    counted_quantity INTEGER,
    variance INTEGER GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED,
    remarks TEXT
);

CREATE INDEX idx_stock_count_items_count ON stock_count_items(stock_count_id);
CREATE INDEX idx_stock_counts_branch ON stock_counts(branch_id);

-- ═══════════════════════════════════════════════════════════════
-- FEATURE 3: Inter-branch stock transfers
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE stock_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    source_branch_id UUID NOT NULL REFERENCES branch(id),
    dest_branch_id UUID NOT NULL REFERENCES branch(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by_id UUID NOT NULL REFERENCES users(id),
    approved_by_id UUID REFERENCES users(id),
    received_by_id UUID REFERENCES users(id),
    transfer_date DATE,
    received_date DATE,
    remarks TEXT,
    CONSTRAINT chk_transfer_different_branches CHECK (source_branch_id <> dest_branch_id)
);

CREATE TABLE stock_transfer_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    stock_transfer_id UUID NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    medicine_batches_id UUID NOT NULL REFERENCES medicine_batches(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    received_quantity INTEGER DEFAULT 0
);

CREATE INDEX idx_stock_transfer_items_transfer ON stock_transfer_items(stock_transfer_id);
CREATE INDEX idx_stock_transfers_source ON stock_transfers(source_branch_id);
CREATE INDEX idx_stock_transfers_dest ON stock_transfers(dest_branch_id);

-- ═══════════════════════════════════════════════════════════════
-- FEATURE 5: Controlled drug register — link to sale items
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE controlled_drugs
    ADD COLUMN sale_items_id UUID REFERENCES sales_items(id),
    ADD COLUMN medicine_batches_id UUID REFERENCES medicine_batches(id),
    ADD COLUMN branch_id UUID REFERENCES branch(id);

-- ═══════════════════════════════════════════════════════════════
-- Seed new permissions for features 2, 3
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.permissions (id, created_at, updated_at, version, permission_name)
SELECT gen_random_uuid(), now(), now(), 0, perm.name
FROM (VALUES
    ('stock_count.read'), ('stock_count.write'),
    ('stock_transfer.read'), ('stock_transfer.write')
) AS perm(name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.permissions p WHERE p.permission_name = perm.name
);

-- Grant stock_count + stock_transfer to OWNER, BRANCH_MANAGER, STORE_KEEPER
INSERT INTO public.role_permissions (id, created_at, updated_at, version, permission_id, role_id)
SELECT gen_random_uuid(), now(), now(), 0, permission.id, role.id
FROM public.permissions permission
JOIN public.user_roles role
  ON (role.role_name IN ('OWNER', 'BRANCH_MANAGER', 'STORE_KEEPER')
      AND permission.permission_name IN (
          'stock_count.read', 'stock_count.write',
          'stock_transfer.read', 'stock_transfer.write'
      ))
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.permission_id = permission.id
      AND existing.role_id = role.id
);
