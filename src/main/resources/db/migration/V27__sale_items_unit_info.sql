ALTER TABLE public.sales_items ADD COLUMN selling_unit_id uuid;
ALTER TABLE public.sales_items ADD COLUMN unit_conversion integer DEFAULT 1;
ALTER TABLE public.sales_items ADD COLUMN original_quantity integer;
