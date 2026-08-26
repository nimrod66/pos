-- Buyer KRA PIN for B2B receipts + duplicate-customer guard.
ALTER TABLE public.customer ADD COLUMN kra_pin varchar(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_kra_phone_backstop
    ON public.customer(pharmacy_id, phone_number)
    WHERE phone_number IS NOT NULL AND phone_number <> '';
