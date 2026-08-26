-- V21: Add pack-size and buying unit to medicine
ALTER TABLE medicine ADD COLUMN pack_size INTEGER;
ALTER TABLE medicine ADD COLUMN buying_unit_id UUID REFERENCES unit_of_measure(id);
