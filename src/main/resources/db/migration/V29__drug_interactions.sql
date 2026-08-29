CREATE TABLE drug_interactions (
    id UUID PRIMARY KEY,
    medicine_1_id UUID NOT NULL REFERENCES medicine(id),
    medicine_2_id UUID NOT NULL REFERENCES medicine(id),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('MINOR', 'MODERATE', 'MAJOR', 'CONTRAINDICATED')),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_drug_interactions_pair UNIQUE (medicine_1_id, medicine_2_id),
    CONSTRAINT chk_drug_interactions_different CHECK (medicine_1_id <> medicine_2_id)
);

CREATE INDEX idx_drug_interactions_m1 ON drug_interactions (medicine_1_id);
CREATE INDEX idx_drug_interactions_m2 ON drug_interactions (medicine_2_id);
CREATE INDEX idx_drug_interactions_active ON drug_interactions (active);
