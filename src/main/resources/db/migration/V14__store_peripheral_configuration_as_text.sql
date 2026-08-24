ALTER TABLE hardware_peripherals
    ALTER COLUMN configuration TYPE text
    USING configuration #>> '{}';
