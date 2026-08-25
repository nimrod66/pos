-- Completes the shared pharmacy reference dictionaries seeded in V9.
-- These tables are read-only through the API (see SecurityConfig), so the
-- missing dosage forms, dispensing units, and product categories ship here.

INSERT INTO public.dosage_form
    (id, version, created_at, updated_at, form_name, form_description)
VALUES
    ('50000000-0000-0000-0000-000000000001', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Tablet', 'Solid oral dose'),
    ('50000000-0000-0000-0000-000000000002', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Capsule', 'Gelatin-encased oral dose'),
    ('50000000-0000-0000-0000-000000000003', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Syrup', 'Liquid oral preparation'),
    ('50000000-0000-0000-0000-000000000004', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Injection', 'Parenteral preparation'),
    ('50000000-0000-0000-0000-000000000005', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Cream', 'Topical semisolid'),
    ('50000000-0000-0000-0000-000000000006', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Suspension', 'Liquid with suspended particles')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.dosage_form (id, version, created_at, updated_at, form_name, form_description)
SELECT gen_random_uuid(), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v.form_name, NULL
FROM (VALUES ('Tablet'), ('Capsule'), ('Syrup'), ('Injection'), ('Cream'), ('Suspension')) AS v(form_name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.dosage_form d WHERE d.form_name = v.form_name
);

INSERT INTO public.unit_of_measure
    (id, version, created_at, updated_at, unit_name, unit_abbreviation)
VALUES
    ('20000000-0000-0000-0000-000000000007', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Strip', 'strip'),
    ('20000000-0000-0000-0000-000000000008', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Box', 'box'),
    ('20000000-0000-0000-0000-000000000009', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Sachet', 'sachet'),
    ('20000000-0000-0000-0000-000000000010', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Ampoule', 'amp')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.unit_of_measure (id, version, created_at, updated_at, unit_name, unit_abbreviation)
SELECT gen_random_uuid(), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v.unit_name, lower(v.unit_name)
FROM (VALUES ('Strip'), ('Box'), ('Sachet'), ('Ampoule')) AS v(unit_name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.unit_of_measure u WHERE u.unit_name = v.unit_name
);

INSERT INTO public.medicine_categories
    (id, version, created_at, updated_at, category_name, category_description)
VALUES
    ('10000000-0000-0000-0000-000000000007', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Antimalarials', 'Malaria treatment and prophylaxis'),
    ('10000000-0000-0000-0000-000000000008', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Vitamins and supplements', 'Vitamins, minerals, and supplements')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.medicine_categories (id, version, created_at, updated_at, category_name, category_description)
SELECT gen_random_uuid(), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, v.category_name, NULL
FROM (VALUES ('Antimalarials'), ('Vitamins and supplements')) AS v(category_name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.medicine_categories c WHERE c.category_name = v.category_name
);
