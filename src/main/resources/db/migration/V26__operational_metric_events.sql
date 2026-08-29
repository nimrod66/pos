CREATE TABLE operational_metric_events (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    pharmacy_id UUID REFERENCES pharmacy(id),
    branch_id UUID REFERENCES branch(id),
    user_id UUID REFERENCES users(id),
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(96),
    source VARCHAR(96),
    terminal_id VARCHAR(96),
    resource_id UUID,
    idempotency_key VARCHAR(160),
    latency_ms BIGINT,
    details TEXT
);

CREATE INDEX idx_operational_metric_created ON operational_metric_events(created_at);
CREATE INDEX idx_operational_metric_type_status ON operational_metric_events(event_type, status);
CREATE INDEX idx_operational_metric_branch_created ON operational_metric_events(branch_id, created_at);
