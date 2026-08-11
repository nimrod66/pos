--
-- PostgreSQL database dump
--

-- Dumped from database version 17.7
-- Dumped by pg_dump version 17.7

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    user_id uuid,
    action character varying(255),
    new_value character varying(255),
    old_value character varying(255),
    record_id character varying(255),
    table_name character varying(255)
);


--
-- Name: branch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.branch (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    pharmacy_id uuid NOT NULL,
    branch_code character varying(255),
    branch_name character varying(255),
    email character varying(255),
    location character varying(255),
    phone_number character varying(255),
    status character varying(255),
    CONSTRAINT branch_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: cash_drawers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cash_drawers (
    actual_closing_balance numeric(38,2),
    closing_time time(0) without time zone,
    expected_closing_balance numeric(38,2),
    opening_balance numeric(38,2),
    opening_time time(0) without time zone,
    variance numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    staff_shifts_id uuid,
    status character varying(255)
);


--
-- Name: cash_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cash_transactions (
    amount numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    cash_drawers_id uuid,
    id uuid NOT NULL,
    reference_id character varying(255),
    reference_type character varying(255),
    remarks character varying(255),
    transaction_type character varying(255)
);


--
-- Name: claim_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claim_attachments (
    created_at timestamp(6) without time zone NOT NULL,
    file_size bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    claim_id uuid NOT NULL,
    id uuid NOT NULL,
    attachment_type character varying(50) NOT NULL,
    file_type character varying(50),
    file_name character varying(200) NOT NULL,
    description character varying(300),
    file_path character varying(500)
);


--
-- Name: claim_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claim_batches (
    claim_count integer,
    total_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    submitted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    status character varying(25),
    batch_reference character varying(50) NOT NULL,
    notes character varying(500),
    CONSTRAINT claim_batches_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'ACKNOWLEDGED'::character varying, 'PROCESSING'::character varying, 'SETTLED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: claim_reconciliations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claim_reconciliations (
    claim_count integer,
    outstanding numeric(15,2),
    period_end date NOT NULL,
    period_start date NOT NULL,
    settled_count integer,
    total_approved numeric(15,2),
    total_claimed numeric(15,2),
    total_paid numeric(15,2),
    total_rejected numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    notes character varying(500)
);


--
-- Name: compliance_batch_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_batch_items (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    batch_id uuid NOT NULL,
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    transmission_status character varying(20),
    kra_receipt_number character varying(100),
    error_message character varying(2000),
    invoice_number character varying(255) NOT NULL
);


--
-- Name: compliance_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_batches (
    invoice_count integer,
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    submitted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    provider character varying(10) NOT NULL,
    id uuid NOT NULL,
    batch_status character varying(20) NOT NULL,
    batch_reference character varying(50) NOT NULL,
    CONSTRAINT compliance_batches_batch_status_check CHECK (((batch_status)::text = ANY ((ARRAY['BUILDING'::character varying, 'SEALED'::character varying, 'SUBMITTING'::character varying, 'SUBMITTED'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'PARTIALLY_FAILED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: compliance_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_certificates (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    valid_from timestamp(6) without time zone NOT NULL,
    valid_to timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    tenant_id uuid,
    status character varying(20) NOT NULL,
    thumbprint character varying(64),
    serial character varying(100) NOT NULL,
    issuer character varying(200) NOT NULL,
    certificate_data text,
    encrypted_private_key text,
    CONSTRAINT compliance_certificates_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying, 'PENDING'::character varying])::text[])))
);


--
-- Name: compliance_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_events (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    actor_id uuid,
    id uuid NOT NULL,
    invoice_id uuid,
    tenant_id uuid,
    event_type character varying(30) NOT NULL,
    terminal_id character varying(36),
    document_number character varying(50),
    correlation_id character varying(64),
    terminal_name character varying(100),
    description character varying(2000),
    actor_name character varying(255),
    payload text,
    CONSTRAINT compliance_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['INVOICE_ISSUED'::character varying, 'INVOICE_SUBMITTED'::character varying, 'INVOICE_REJECTED'::character varying, 'INVOICE_CANCELLED'::character varying, 'RECEIPT_PRINTED'::character varying, 'RECEIPT_REPRINTED'::character varying, 'CREDIT_NOTE_ISSUED'::character varying, 'DEBIT_NOTE_ISSUED'::character varying, 'RETRY_SCHEDULED'::character varying, 'DEAD_LETTER_REACHED'::character varying, 'CERTIFICATE_ROTATED'::character varying, 'RECONCILIATION_STARTED'::character varying, 'RECONCILIATION_COMPLETED'::character varying, 'TRANSMISSION_ATTEMPTED'::character varying, 'TRANSMISSION_SUCCEEDED'::character varying, 'TRANSMISSION_FAILED'::character varying])::text[])))
);


--
-- Name: controlled_drugs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.controlled_drugs (
    quantity_dispensed integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid,
    prescriptions_id uuid,
    user_id uuid
);


--
-- Name: credit_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credit_notes (
    amount numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    issue_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    created_by uuid,
    id uuid NOT NULL,
    original_invoice_id uuid NOT NULL,
    tenant_id uuid,
    reason character varying(1000),
    credit_note_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT credit_notes_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ISSUED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    loyalty_points integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    address character varying(255),
    email character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    notes character varying(255),
    phone_number character varying(255)
);


--
-- Name: dead_letter_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dead_letter_records (
    attempts_exhausted integer,
    assigned_to bigint,
    created_at timestamp(6) without time zone NOT NULL,
    invoice_id bigint,
    resolved_at timestamp(6) without time zone,
    tenant_id bigint,
    transmission_id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    document_number character varying(50),
    resolution character varying(2000),
    failure_reason character varying(4000),
    last_kra_response text,
    CONSTRAINT dead_letter_records_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_REVIEW'::character varying, 'RETRYING'::character varying, 'RESOLVED'::character varying, 'DISCARDED'::character varying])::text[])))
);


--
-- Name: debit_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.debit_notes (
    amount numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    issue_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    created_by uuid,
    id uuid NOT NULL,
    original_invoice_id uuid NOT NULL,
    tenant_id uuid,
    reason character varying(1000),
    debit_note_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT debit_notes_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ISSUED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: device_registration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_registration (
    created_at timestamp(6) without time zone NOT NULL,
    last_renewed_at timestamp(6) without time zone,
    registered_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    tenant_id uuid,
    environment character varying(20) NOT NULL,
    kra_pin character varying(20) NOT NULL,
    registration_status character varying(20) NOT NULL,
    device_serial character varying(50) NOT NULL,
    encrypted_cmc_key text NOT NULL
);


--
-- Name: dispensed_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dispensed_items (
    dispensed_quantity integer,
    created_at timestamp(6) without time zone NOT NULL,
    dispensing_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    prescription_items_id uuid,
    user_id uuid
);


--
-- Name: document_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.document_sequences (
    created_at timestamp(6) without time zone NOT NULL,
    last_sequence bigint NOT NULL,
    sequence_date character varying(8) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    branch_code character varying(20) NOT NULL,
    document_type character varying(30) NOT NULL
);


--
-- Name: dosage_form; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dosage_form (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    form_description character varying(255),
    form_name character varying(255)
);


--
-- Name: efris_fiscal_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.efris_fiscal_documents (
    country_code character varying(2) NOT NULL,
    currency character varying(3),
    discount numeric(15,2),
    grand_total numeric(15,2),
    schema_version integer,
    subtotal numeric(15,2),
    tax_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    issue_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    created_by uuid,
    id uuid NOT NULL,
    sale_id uuid,
    tenant_id uuid,
    buyer_tin character varying(20),
    document_status character varying(20) NOT NULL,
    provider_code character varying(20) NOT NULL,
    tin character varying(20),
    document_number character varying(50) NOT NULL,
    efris_invoice_number character varying(50),
    qr_image_path character varying(500),
    verification_url character varying(1000),
    qr_code_content character varying(2000),
    raw_response text
);


--
-- Name: etims_fiscal_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etims_fiscal_documents (
    country_code character varying(2) NOT NULL,
    currency character varying(3),
    discount numeric(15,2),
    grand_total numeric(15,2),
    schema_version integer,
    subtotal numeric(15,2),
    tax_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    issue_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    created_by uuid,
    id uuid NOT NULL,
    sale_id uuid,
    tenant_id uuid,
    transmission_id uuid,
    customer_pin character varying(20),
    document_status character varying(20) NOT NULL,
    kra_pin character varying(20),
    provider_code character varying(20) NOT NULL,
    supplier_pin character varying(20),
    control_unit_serial character varying(50),
    document_number character varying(50) NOT NULL,
    invoice_number character varying(50),
    receipt_code character varying(50),
    qr_image_path character varying(500),
    verification_url character varying(1000),
    qr_code_content character varying(2000),
    raw_response text
);


--
-- Name: expense_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense_category (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    category_description character varying(255),
    category_name character varying(255)
);


--
-- Name: expenses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expenses (
    amount numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    expense_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    cash_drawers_id uuid,
    expense_category_id uuid,
    id uuid NOT NULL,
    user_id uuid,
    description character varying(255)
);


--
-- Name: expiry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expiry (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    user_id uuid,
    disposal_method character varying(255)
);


--
-- Name: fiscal_years; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fiscal_years (
    end_date date NOT NULL,
    is_current boolean,
    start_date date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    tenant_id uuid,
    year_code character varying(20) NOT NULL
);


--
-- Name: goods_received_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.goods_received_notes (
    created_at timestamp(6) without time zone NOT NULL,
    received_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    purchase_orders_id uuid,
    received_by_user_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    idempotency_key character varying(64),
    supplier_invoice_number character varying(100),
    remarks character varying(500)
);


--
-- Name: grn_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grn_lines (
    expiry_date date,
    quantity integer NOT NULL,
    unit_cost numeric(15,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    batch_id uuid,
    grn_id uuid NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid NOT NULL,
    purchase_order_line_id uuid,
    batch_number character varying(100) NOT NULL
);


--
-- Name: hardware_peripherals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hardware_peripherals (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    terminal_id uuid NOT NULL,
    connection_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    type character varying(30) NOT NULL,
    manufacturer character varying(50),
    model character varying(50),
    configuration json,
    CONSTRAINT hardware_peripherals_connection_type_check CHECK (((connection_type)::text = ANY ((ARRAY['NETWORK'::character varying, 'USB'::character varying, 'BLUETOOTH'::character varying, 'SERIAL'::character varying, 'WEDGE'::character varying, 'PRINTER_PORT'::character varying])::text[]))),
    CONSTRAINT hardware_peripherals_status_check CHECK (((status)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying, 'UNKNOWN'::character varying, 'ERROR'::character varying])::text[]))),
    CONSTRAINT hardware_peripherals_type_check CHECK (((type)::text = ANY ((ARRAY['PRINTER'::character varying, 'SCANNER'::character varying, 'CASH_DRAWER'::character varying, 'SCALE'::character varying, 'DISPLAY'::character varying, 'FINGERPRINT'::character varying, 'NFC'::character varying, 'CAMERA'::character varying, 'RFID'::character varying, 'SECOND_DISPLAY'::character varying, 'BARCODE_PRINTER'::character varying])::text[])))
);


--
-- Name: idempotency; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.idempotency (
    created_time time(0) without time zone,
    expires_at time(0) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    idempotency_key character varying(255),
    request_hash character varying(255),
    resource_id character varying(255),
    resource_type character varying(255),
    status character varying(255),
    CONSTRAINT idempotency_status_check CHECK (((status)::text = ANY ((ARRAY['IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: insurance_authorizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_authorizations (
    approved_amount numeric(15,2),
    used_amount numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    expiry_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    status character varying(25),
    authorization_reference character varying(50) NOT NULL,
    authorized_by character varying(100),
    notes character varying(500),
    CONSTRAINT insurance_authorizations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXHAUSTED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[])))
);


--
-- Name: insurance_claims; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_claims (
    approved_amount numeric(15,2),
    claim_amount numeric(15,2) NOT NULL,
    co_pay_amount numeric(15,2) NOT NULL,
    rejected_amount numeric(15,2),
    sale_total numeric(15,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    settled_at timestamp(6) without time zone,
    submitted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    authorization_id uuid,
    batch_id uuid,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    member_id uuid,
    payment_id uuid,
    sale_id uuid NOT NULL,
    scheme_id uuid,
    claim_status character varying(25) NOT NULL,
    claim_reference character varying(50),
    patient_membership_id character varying(50),
    patient_name character varying(100),
    notes character varying(500),
    rejection_reason character varying(500),
    CONSTRAINT insurance_claims_claim_status_check CHECK (((claim_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PREAUTH_OBTAINED'::character varying, 'SUBMITTED'::character varying, 'ACKNOWLEDGED'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying, 'REJECTED'::character varying, 'APPEALED'::character varying, 'WRITTEN_OFF'::character varying])::text[])))
);


--
-- Name: insurance_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_members (
    expiry_date date,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    national_id character varying(20),
    phone character varying(20),
    status character varying(20),
    membership_number character varying(50) NOT NULL,
    member_name character varying(100),
    notes character varying(500),
    CONSTRAINT insurance_members_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXPIRED'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: insurance_payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_payments (
    amount numeric(15,2) NOT NULL,
    payment_date date,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    payment_method character varying(30),
    bank_reference character varying(50),
    payment_reference character varying(50) NOT NULL,
    notes character varying(500),
    receipt_path character varying(500),
    CONSTRAINT insurance_payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['BANK_TRANSFER'::character varying, 'CHEQUE'::character varying, 'M_PESA'::character varying, 'CASH'::character varying])::text[])))
);


--
-- Name: insurance_schemes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurance_schemes (
    co_pay_flat numeric(15,2),
    co_pay_percentage numeric(5,2),
    max_claim_amount numeric(15,2),
    requires_preauth boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_id uuid NOT NULL,
    status character varying(20),
    code character varying(50),
    name character varying(100) NOT NULL,
    description character varying(500),
    CONSTRAINT insurance_schemes_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: insurers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insurers (
    default_co_pay_flat numeric(15,2),
    default_co_pay_percentage numeric(5,2),
    max_claim_amount numeric(15,2),
    requires_preauth boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    insurer_type character varying(20) NOT NULL,
    phone_number character varying(20),
    preauth_phone character varying(20),
    status character varying(20),
    code character varying(50),
    claim_submission_email character varying(100),
    contact_person character varying(100),
    email character varying(100),
    name character varying(100) NOT NULL,
    CONSTRAINT insurers_insurer_type_check CHECK (((insurer_type)::text = ANY ((ARRAY['GOVERNMENT'::character varying, 'PRIVATE'::character varying, 'CORPORATE'::character varying, 'SELF_PAY'::character varying])::text[]))),
    CONSTRAINT insurers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: invoice_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_history (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    actor_id uuid,
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    history_type character varying(30) NOT NULL,
    description character varying(2000),
    metadata character varying(4000),
    actor_name character varying(255),
    CONSTRAINT invoice_history_history_type_check CHECK (((history_type)::text = ANY ((ARRAY['CREATED'::character varying, 'ISSUED'::character varying, 'SENT_TO_KRA'::character varying, 'ACKNOWLEDGED'::character varying, 'REPRINTED'::character varying, 'CREDIT_NOTE_ISSUED'::character varying, 'DEBIT_NOTE_ISSUED'::character varying, 'VOID'::character varying, 'CLOSED'::character varying, 'TRANSMISSION_FAILED'::character varying])::text[])))
);


--
-- Name: kra_code_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_code_list (
    active boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    code_type character varying(50) NOT NULL,
    code_value character varying(50) NOT NULL,
    code_name character varying(200) NOT NULL,
    description character varying(500),
    kra_code character varying(255)
);


--
-- Name: kra_county_code; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_county_code (
    active boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    county_code character varying(10) NOT NULL,
    id uuid NOT NULL,
    county_name character varying(100) NOT NULL
);


--
-- Name: kra_item_classification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_item_classification (
    active boolean,
    level integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    classification_code character varying(30) NOT NULL,
    parent_code character varying(30),
    classification_name character varying(200) NOT NULL
);


--
-- Name: kra_notice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_notice (
    acknowledged boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    notice_date character varying(20),
    notice_number character varying(50),
    title character varying(500) NOT NULL,
    content text
);


--
-- Name: kra_packaging_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_packaging_type (
    active boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    code character varying(10) NOT NULL,
    id uuid NOT NULL,
    name character varying(100) NOT NULL
);


--
-- Name: kra_unit_of_measure; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kra_unit_of_measure (
    active boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    code character varying(10) NOT NULL,
    id uuid NOT NULL,
    name character varying(100) NOT NULL
);


--
-- Name: login_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.login_history (
    created_at timestamp(6) without time zone NOT NULL,
    login_time timestamp(6) without time zone,
    logout_time timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    browser character varying(255),
    device character varying(255),
    ip_address character varying(255)
);


--
-- Name: manufacturer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manufacturer (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    manufacturer_contact character varying(255),
    manufacturer_country character varying(255),
    manufacturer_name character varying(255)
);


--
-- Name: medicine; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medicine (
    is_controlled_drug boolean NOT NULL,
    maximum_dispense_quantity integer,
    requires_prescription boolean NOT NULL,
    requires_refrigeration boolean NOT NULL,
    track_batch boolean,
    track_expiry boolean,
    track_serial_number boolean,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    dosage_form_id uuid,
    id uuid NOT NULL,
    manufacturer_id uuid,
    medicine_categories_id uuid,
    tax_category_id uuid,
    unit_of_measure_id uuid,
    barcode_source character varying(20),
    barcode_type character varying(20),
    gs1_company_prefix character varying(20),
    etims_item_code character varying(50),
    internal_barcode character varying(50),
    kemsa_code character varying(50),
    manufacturer_barcode character varying(50),
    ppb_code character varying(50),
    barcode character varying(255),
    brand_name character varying(255),
    description character varying(255),
    generic_name character varying(255),
    minimum_age character varying(255),
    sku character varying(255),
    status character varying(255),
    strength character varying(255),
    CONSTRAINT medicine_barcode_source_check CHECK (((barcode_source)::text = ANY ((ARRAY['MANUFACTURER'::character varying, 'SYSTEM_GENERATED'::character varying, 'KEMSA'::character varying, 'MEDS'::character varying, 'PPB'::character varying, 'CUSTOM'::character varying, 'WHOLESALER'::character varying])::text[]))),
    CONSTRAINT medicine_barcode_type_check CHECK (((barcode_type)::text = ANY ((ARRAY['EAN13'::character varying, 'EAN8'::character varying, 'UPC_A'::character varying, 'UPC_E'::character varying, 'CODE128'::character varying, 'CODE39'::character varying, 'CODE93'::character varying, 'ITF14'::character varying, 'QR_CODE'::character varying, 'GS1_DATAMATRIX'::character varying, 'GS1_128'::character varying, 'CODABAR'::character varying, 'PDF417'::character varying])::text[]))),
    CONSTRAINT medicine_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'NOT_AVAILABLE'::character varying])::text[])))
);


--
-- Name: medicine_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medicine_batches (
    buying_price numeric(38,2),
    expiration_date date,
    initial_quantity integer,
    manufacture_date date,
    selling_price numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid,
    batch_number character varying(255)
);


--
-- Name: medicine_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medicine_categories (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    category_description character varying(255),
    category_name character varying(255)
);


--
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    id uuid NOT NULL,
    reference_id uuid,
    user_id uuid,
    message character varying(255),
    reference_type character varying(255),
    status character varying(255),
    title character varying(255),
    type character varying(255),
    CONSTRAINT notification_status_check CHECK (((status)::text = ANY ((ARRAY['UNREAD'::character varying, 'READ'::character varying, 'DISMISSED'::character varying])::text[]))),
    CONSTRAINT notification_type_check CHECK (((type)::text = ANY ((ARRAY['LOW_STOCK'::character varying, 'EXPIRY_WARNING'::character varying, 'SALE_COMPLETED'::character varying, 'SHIFT_REMINDER'::character varying, 'SYSTEM_ALERT'::character varying])::text[])))
);


--
-- Name: outbox_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbox_events (
    created_at timestamp(6) without time zone NOT NULL,
    published_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    aggregate_id character varying(255) NOT NULL,
    aggregate_type character varying(255) NOT NULL,
    event_type character varying(255) NOT NULL,
    payload text,
    status character varying(255) NOT NULL,
    CONSTRAINT outbox_events_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    amount numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    payment_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    sales_id uuid,
    currency character varying(255),
    description character varying(255),
    payment_method character varying(255),
    payment_status character varying(255),
    transaction_reference character varying(255),
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['M_PESA'::character varying, 'CASH'::character varying, 'CARD'::character varying, 'STRIPE'::character varying])::text[])))
);


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    action_name character varying(255),
    description character varying(255),
    module_name character varying(255),
    permission_name character varying(255)
);


--
-- Name: pharmacy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pharmacy (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    address character varying(255),
    email character varying(255),
    kra_pin character varying(255),
    license_number character varying(255),
    name character varying(255),
    phone_number character varying(255)
);


--
-- Name: prescription_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prescription_items (
    quantity integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid,
    prescription_id uuid,
    dosage character varying(255)
);


--
-- Name: prescriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prescriptions (
    issued_date date,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    customer_name character varying(255),
    diagnosis character varying(255),
    doctor_license_number character varying(255),
    doctor_name character varying(255),
    hospital_name character varying(255),
    prescription_number character varying(255),
    status character varying(255)
);


--
-- Name: price_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.price_history (
    new_buying_price numeric(38,2),
    new_selling_price numeric(38,2),
    old_buying_price numeric(38,2),
    old_selling_price numeric(38,2),
    changed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    medicine_id uuid,
    user_id uuid
);


--
-- Name: purchase_order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchase_order_items (
    buying_price numeric(38,2),
    discount numeric(38,2),
    quantity integer,
    tax numeric(38,2),
    total numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid,
    purchase_orders_id uuid
);


--
-- Name: purchase_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchase_orders (
    created_at timestamp(6) without time zone NOT NULL,
    delivery_date timestamp(6) without time zone,
    expected_delivery_date timestamp(6) without time zone,
    order_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    approved_by uuid,
    branch_id uuid,
    id uuid NOT NULL,
    suppliers_id uuid,
    user_id uuid,
    status character varying(255),
    CONSTRAINT purchase_orders_status_check CHECK (((status)::text = ANY ((ARRAY['ORDERED'::character varying, 'DELIVERED'::character varying, 'IN_PROGRESS'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receipts (
    created_at timestamp(6) without time zone NOT NULL,
    printed_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    sales_id uuid,
    receipt_number character varying(255)
);


--
-- Name: receipts_compliance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.receipts_compliance (
    reprint_count integer,
    created_at timestamp(6) without time zone NOT NULL,
    printed_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    invoice_id uuid,
    sale_id uuid NOT NULL,
    tenant_id uuid,
    kra_pin character varying(20),
    receipt_number character varying(50) NOT NULL,
    verification_url character varying(1000),
    qr_code_content character varying(2000),
    business_name character varying(255),
    receipt_data text
);


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    permission_id uuid NOT NULL,
    role_id uuid NOT NULL
);


--
-- Name: sale_return_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sale_return_items (
    quantity integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    sale_items_id uuid,
    sale_returns_id uuid
);


--
-- Name: sale_returns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sale_returns (
    created_at timestamp(6) without time zone NOT NULL,
    return_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    sales_id uuid,
    user_id uuid,
    reason character varying(255),
    status character varying(255)
);


--
-- Name: sales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sales (
    subtotal numeric(38,2),
    synced boolean NOT NULL,
    tax numeric(38,2),
    total numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    customer_id uuid,
    id uuid NOT NULL,
    idempotency_id uuid,
    user_id uuid,
    uuid character varying(36) NOT NULL,
    invoice_number character varying(255),
    payment_status character varying(255),
    sale_status character varying(255),
    terminal_id character varying(255),
    CONSTRAINT sales_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PAID'::character varying, 'NOT_PAID'::character varying, 'IN_PROGRESS'::character varying])::text[]))),
    CONSTRAINT sales_sale_status_check CHECK (((sale_status)::text = ANY ((ARRAY['DONE'::character varying, 'CANCELLED'::character varying, 'SUSPENDED'::character varying])::text[])))
);


--
-- Name: sales_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sales_items (
    discount numeric(38,2),
    price numeric(38,2),
    quantity integer,
    tax numeric(38,2),
    tax_rate numeric(38,2),
    taxable_amount numeric(38,2),
    total numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    sales_id uuid
);


--
-- Name: staff_shifts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.staff_shifts (
    shift_number integer,
    created_at timestamp(6) without time zone NOT NULL,
    shift_end_time timestamp(6) without time zone,
    shift_start_time timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid NOT NULL,
    id uuid NOT NULL,
    role_id uuid,
    user_id uuid NOT NULL,
    remarks character varying(255),
    shift_name character varying(255),
    status character varying(255),
    CONSTRAINT staff_shifts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'CLOSED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: stock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock (
    last_stock_date date,
    maximum_stock integer,
    minimum_stock integer,
    quantity_available integer,
    reorder_level integer,
    reserved_quantity integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    shelf_location character varying(255)
);


--
-- Name: stock_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_movements (
    movement_date date,
    quantity integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    id uuid NOT NULL,
    medicine_batches_id uuid,
    reference_id uuid,
    user_id uuid,
    movement_type character varying(255),
    reference_type character varying(255),
    CONSTRAINT stock_movements_movement_type_check CHECK (((movement_type)::text = ANY ((ARRAY['PURCHASE'::character varying, 'SALE'::character varying, 'RETURN'::character varying, 'TRANSFER'::character varying, 'ADJUSTMENT'::character varying, 'EXPIRED'::character varying, 'DAMAGED'::character varying, 'LOSS'::character varying, 'DISPENSE'::character varying, 'RESERVATION'::character varying, 'RESERVATION_RELEASE'::character varying])::text[])))
);


--
-- Name: supplier_catalog_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplier_catalog_items (
    unit_price numeric(15,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    match_confidence character varying(10),
    catalog_id uuid NOT NULL,
    id uuid NOT NULL,
    matched_medicine_id uuid,
    atc_code character varying(20),
    pack_size character varying(20),
    unit_of_measure character varying(30),
    barcode character varying(50),
    dosage_form character varying(50),
    etims_classification_code character varying(50),
    manufacturer_country character varying(50),
    strength character varying(50),
    supplier_code character varying(50) NOT NULL,
    manufacturer_name character varying(150),
    generic_name character varying(200),
    product_name character varying(200)
);


--
-- Name: supplier_catalogs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplier_catalogs (
    total_items integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    catalog_version character varying(20),
    status character varying(20),
    supplier character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    source_url character varying(500)
);


--
-- Name: supplier_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplier_invoices (
    balance_due numeric(38,2),
    sub_total numeric(38,2),
    tax numeric(38,2),
    total numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    invoice_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    suppliers_id uuid,
    invoice_number character varying(255),
    status character varying(255),
    CONSTRAINT supplier_invoices_status_check CHECK (((status)::text = ANY ((ARRAY['PAID'::character varying, 'NOT_PAID'::character varying])::text[])))
);


--
-- Name: supplier_payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplier_payment (
    payment_amount numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    payment_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    supplier_invoices_id uuid,
    user_id uuid,
    payment_method character varying(255),
    payment_reference character varying(255)
);


--
-- Name: suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suppliers (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    address character varying(255),
    contact_person character varying(255),
    email character varying(255),
    license_number character varying(255),
    payment_terms character varying(255),
    phone_number character varying(255),
    status character varying(255),
    supplier_name character varying(255),
    CONSTRAINT suppliers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: sync_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sync_outbox (
    aggregate_version integer NOT NULL,
    event_version integer NOT NULL,
    retry_count integer NOT NULL,
    sequence_number integer NOT NULL,
    acknowledged_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    next_retry_at timestamp(6) without time zone,
    sent_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    aggregate_id character varying(36) NOT NULL,
    event_id character varying(36) NOT NULL,
    terminal_id character varying(36) NOT NULL,
    event_type character varying(40) NOT NULL,
    aggregate_type character varying(50) NOT NULL,
    last_error character varying(1000),
    payload text,
    CONSTRAINT sync_outbox_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['SALE_CREATED'::character varying, 'SALE_CANCELLED'::character varying, 'SALE_SUSPENDED'::character varying, 'SALE_RESUMED'::character varying, 'PAYMENT_RECEIVED'::character varying, 'PAYMENT_REFUNDED'::character varying, 'SALE_RETURNED'::character varying, 'STOCK_RECEIVED'::character varying, 'STOCK_DEDUCTED'::character varying, 'STOCK_ADJUSTED'::character varying, 'STOCK_MOVEMENT'::character varying, 'PRODUCT_CREATED'::character varying, 'PRODUCT_UPDATED'::character varying, 'PRODUCT_DELETED'::character varying, 'PRICE_UPDATED'::character varying, 'CUSTOMER_CREATED'::character varying, 'CUSTOMER_UPDATED'::character varying, 'SHIFT_OPENED'::character varying, 'SHIFT_CLOSED'::character varying, 'BRANCH_REGISTERED'::character varying, 'TERMINAL_REGISTERED'::character varying])::text[]))),
    CONSTRAINT sync_outbox_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying, 'DEAD'::character varying, 'IGNORED'::character varying])::text[])))
);


--
-- Name: sync_state; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sync_state (
    records_failed integer,
    records_synced integer,
    created_at timestamp(6) without time zone NOT NULL,
    last_sync_at timestamp(6) without time zone,
    tenant_id bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    last_sync_status character varying(20),
    sync_type character varying(30) NOT NULL,
    error_message character varying(1000)
);


--
-- Name: system_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_settings (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    id uuid NOT NULL,
    pharmacy_id uuid NOT NULL,
    description character varying(1000),
    setting_value character varying(4000),
    setting_key character varying(255) NOT NULL
);


--
-- Name: tax_category; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_category (
    active boolean NOT NULL,
    tax_rate numeric(38,2),
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    tax_description character varying(255),
    tax_name character varying(255),
    tax_type character varying(255),
    CONSTRAINT tax_category_tax_type_check CHECK (((tax_type)::text = ANY ((ARRAY['VAT_STANDARD'::character varying, 'VAT_REDUCED'::character varying, 'VAT_ZERO'::character varying, 'EXEMPT'::character varying, 'OUT_OF_SCOPE'::character varying])::text[])))
);


--
-- Name: tax_invoice_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_invoice_items (
    discount numeric(15,2),
    quantity integer NOT NULL,
    subtotal numeric(15,2) NOT NULL,
    tax_amount numeric(15,2),
    tax_rate numeric(10,4),
    taxable_amount numeric(15,2),
    total numeric(15,2) NOT NULL,
    unit_price numeric(15,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    medicine_id uuid,
    tax_invoice_id uuid NOT NULL,
    barcode_type character varying(20),
    etims_classification_code character varying(50),
    barcode character varying(255),
    medicine_name character varying(255),
    tax_type character varying(255)
);


--
-- Name: tax_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_invoices (
    currency character varying(3),
    discount numeric(15,2),
    grand_total numeric(15,2) NOT NULL,
    schema_version integer,
    subtotal numeric(15,2) NOT NULL,
    tax_amount numeric(15,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    issue_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid,
    customer_id uuid,
    id uuid NOT NULL,
    sale_id uuid,
    tenant_id uuid,
    customer_pin character varying(20),
    branch_code character varying(30),
    invoice_status character varying(30) NOT NULL,
    invoice_number character varying(50) NOT NULL,
    qr_image_path character varying(500),
    verification_url character varying(1000),
    qr_code_content character varying(2000),
    customer_name character varying(255),
    CONSTRAINT tax_invoices_invoice_status_check CHECK (((invoice_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ISSUED'::character varying, 'VOID'::character varying, 'CREDITED'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: tax_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_periods (
    end_date date NOT NULL,
    is_submitted boolean,
    start_date date NOT NULL,
    submission_deadline date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    tenant_id uuid,
    period_code character varying(20) NOT NULL
);


--
-- Name: terminal_configuration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminal_configuration (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    terminal_id uuid NOT NULL,
    config_key character varying(100) NOT NULL,
    description character varying(500),
    config_value character varying(1000)
);


--
-- Name: terminal_heartbeats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminal_heartbeats (
    battery_charging boolean,
    battery_level integer,
    signal_strength integer,
    created_at timestamp(6) without time zone NOT NULL,
    "timestamp" timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    uptime_minutes bigint,
    version bigint NOT NULL,
    id uuid NOT NULL,
    terminal_id uuid NOT NULL,
    network_type character varying(20),
    peripheral_status json,
    additional_metrics json
);


--
-- Name: terminal_registry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminal_registry (
    migrated_from_terminal boolean,
    created_at timestamp(6) without time zone NOT NULL,
    last_seen_at timestamp(6) without time zone,
    last_update timestamp(6) without time zone,
    registered_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    api_secret character varying(16) NOT NULL,
    branch_id uuid,
    id uuid NOT NULL,
    status character varying(20) NOT NULL,
    app_version character varying(30),
    minimum_backend_version character varying(30),
    os_version character varying(30),
    platform character varying(30),
    supported_api_version character varying(30),
    terminal_type character varying(30) NOT NULL,
    terminal_id character varying(36) NOT NULL,
    firmware_version character varying(50),
    manufacturer character varying(50),
    model character varying(50),
    registered_by character varying(50),
    name character varying(100) NOT NULL,
    serial_number character varying(100),
    api_key character varying(128) NOT NULL,
    CONSTRAINT terminal_registry_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'DEACTIVATED'::character varying, 'BLOCKED'::character varying])::text[]))),
    CONSTRAINT terminal_registry_terminal_type_check CHECK (((terminal_type)::text = ANY ((ARRAY['WEB'::character varying, 'WINDOWS'::character varying, 'ANDROID_HANDHELD'::character varying, 'ANDROID_TABLET'::character varying, 'IOS'::character varying, 'API'::character varying])::text[])))
);


--
-- Name: terminal_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminal_sessions (
    active boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone,
    last_activity_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    cashier_id uuid,
    id uuid NOT NULL,
    terminal_id uuid NOT NULL,
    session_id character varying(36) NOT NULL,
    ip_address character varying(45),
    token character varying(500) NOT NULL,
    user_agent character varying(500)
);


--
-- Name: terminals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terminals (
    active boolean NOT NULL,
    synced boolean NOT NULL,
    registered_at timestamp(6) without time zone,
    api_secret character varying(16) NOT NULL,
    branch_id character varying(36),
    terminal_id character varying(36) NOT NULL,
    name character varying(100) NOT NULL,
    api_key character varying(128) NOT NULL
);


--
-- Name: transmission_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transmission_attempts (
    attempt_number integer NOT NULL,
    status_code integer,
    success boolean,
    created_at timestamp(6) without time zone NOT NULL,
    duration_ms bigint,
    response_at timestamp(6) without time zone,
    sent_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    transmission_id uuid NOT NULL,
    error_message character varying(4000),
    request_payload text,
    response_payload text
);


--
-- Name: transmissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transmissions (
    payload_version integer,
    created_at timestamp(6) without time zone NOT NULL,
    next_retry_time timestamp(6) without time zone,
    submitted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    submitted_by uuid,
    tenant_id uuid,
    transmission_status character varying(20) NOT NULL,
    document_type character varying(30) NOT NULL,
    idempotency_key character varying(64),
    request_hash character varying(64),
    response_hash character varying(64),
    kra_receipt_number character varying(100),
    failure_reason character varying(2000),
    kra_request text,
    kra_response text,
    CONSTRAINT transmissions_transmission_status_check CHECK (((transmission_status)::text = ANY ((ARRAY['PENDING'::character varying, 'TRANSMITTING'::character varying, 'FAILED'::character varying, 'TRANSMITTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: unit_of_measure; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.unit_of_measure (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    unit_abbreviation character varying(255),
    unit_name character varying(255)
);


--
-- Name: user_branch_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_branch_role (
    assigned_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    assigned_by uuid NOT NULL,
    branch_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    user_roles_id uuid NOT NULL
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    id uuid NOT NULL,
    description character varying(500),
    role_name character varying(255)
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    created_at timestamp(6) without time zone NOT NULL,
    last_login timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    branch_id uuid NOT NULL,
    id uuid NOT NULL,
    email character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    middle_name character varying(255),
    password_hash character varying(255),
    phone_number character varying(255),
    status character varying(255),
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'TRANSFERRED'::character varying])::text[])))
);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: branch branch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT branch_pkey PRIMARY KEY (id);


--
-- Name: cash_drawers cash_drawers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_drawers
    ADD CONSTRAINT cash_drawers_pkey PRIMARY KEY (id);


--
-- Name: cash_transactions cash_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_transactions
    ADD CONSTRAINT cash_transactions_pkey PRIMARY KEY (id);


--
-- Name: claim_attachments claim_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_attachments
    ADD CONSTRAINT claim_attachments_pkey PRIMARY KEY (id);


--
-- Name: claim_batches claim_batches_batch_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_batches
    ADD CONSTRAINT claim_batches_batch_reference_key UNIQUE (batch_reference);


--
-- Name: claim_batches claim_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_batches
    ADD CONSTRAINT claim_batches_pkey PRIMARY KEY (id);


--
-- Name: claim_reconciliations claim_reconciliations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_reconciliations
    ADD CONSTRAINT claim_reconciliations_pkey PRIMARY KEY (id);


--
-- Name: compliance_batch_items compliance_batch_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_batch_items
    ADD CONSTRAINT compliance_batch_items_pkey PRIMARY KEY (id);


--
-- Name: compliance_batches compliance_batches_batch_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_batches
    ADD CONSTRAINT compliance_batches_batch_reference_key UNIQUE (batch_reference);


--
-- Name: compliance_batches compliance_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_batches
    ADD CONSTRAINT compliance_batches_pkey PRIMARY KEY (id);


--
-- Name: compliance_certificates compliance_certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_certificates
    ADD CONSTRAINT compliance_certificates_pkey PRIMARY KEY (id);


--
-- Name: compliance_certificates compliance_certificates_serial_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_certificates
    ADD CONSTRAINT compliance_certificates_serial_key UNIQUE (serial);


--
-- Name: compliance_events compliance_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_events
    ADD CONSTRAINT compliance_events_pkey PRIMARY KEY (id);


--
-- Name: controlled_drugs controlled_drugs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.controlled_drugs
    ADD CONSTRAINT controlled_drugs_pkey PRIMARY KEY (id);


--
-- Name: credit_notes credit_notes_credit_note_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT credit_notes_credit_note_number_key UNIQUE (credit_note_number);


--
-- Name: credit_notes credit_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT credit_notes_pkey PRIMARY KEY (id);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);


--
-- Name: dead_letter_records dead_letter_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dead_letter_records
    ADD CONSTRAINT dead_letter_records_pkey PRIMARY KEY (id);


--
-- Name: debit_notes debit_notes_debit_note_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.debit_notes
    ADD CONSTRAINT debit_notes_debit_note_number_key UNIQUE (debit_note_number);


--
-- Name: debit_notes debit_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.debit_notes
    ADD CONSTRAINT debit_notes_pkey PRIMARY KEY (id);


--
-- Name: device_registration device_registration_device_serial_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_registration
    ADD CONSTRAINT device_registration_device_serial_key UNIQUE (device_serial);


--
-- Name: device_registration device_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_registration
    ADD CONSTRAINT device_registration_pkey PRIMARY KEY (id);


--
-- Name: dispensed_items dispensed_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispensed_items
    ADD CONSTRAINT dispensed_items_pkey PRIMARY KEY (id);


--
-- Name: document_sequences document_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_sequences
    ADD CONSTRAINT document_sequences_pkey PRIMARY KEY (id);


--
-- Name: dosage_form dosage_form_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dosage_form
    ADD CONSTRAINT dosage_form_pkey PRIMARY KEY (id);


--
-- Name: efris_fiscal_documents efris_fiscal_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.efris_fiscal_documents
    ADD CONSTRAINT efris_fiscal_documents_pkey PRIMARY KEY (id);


--
-- Name: etims_fiscal_documents etims_fiscal_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etims_fiscal_documents
    ADD CONSTRAINT etims_fiscal_documents_pkey PRIMARY KEY (id);


--
-- Name: expense_category expense_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_category
    ADD CONSTRAINT expense_category_pkey PRIMARY KEY (id);


--
-- Name: expenses expenses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);


--
-- Name: expiry expiry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expiry
    ADD CONSTRAINT expiry_pkey PRIMARY KEY (id);


--
-- Name: fiscal_years fiscal_years_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_years
    ADD CONSTRAINT fiscal_years_pkey PRIMARY KEY (id);


--
-- Name: fiscal_years fiscal_years_year_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_years
    ADD CONSTRAINT fiscal_years_year_code_key UNIQUE (year_code);


--
-- Name: goods_received_notes goods_received_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_received_notes
    ADD CONSTRAINT goods_received_notes_pkey PRIMARY KEY (id);


--
-- Name: grn_lines grn_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grn_lines
    ADD CONSTRAINT grn_lines_pkey PRIMARY KEY (id);


--
-- Name: hardware_peripherals hardware_peripherals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hardware_peripherals
    ADD CONSTRAINT hardware_peripherals_pkey PRIMARY KEY (id);


--
-- Name: idempotency idempotency_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idempotency
    ADD CONSTRAINT idempotency_pkey PRIMARY KEY (id);


--
-- Name: insurance_authorizations insurance_authorizations_authorization_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_authorizations
    ADD CONSTRAINT insurance_authorizations_authorization_reference_key UNIQUE (authorization_reference);


--
-- Name: insurance_authorizations insurance_authorizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_authorizations
    ADD CONSTRAINT insurance_authorizations_pkey PRIMARY KEY (id);


--
-- Name: insurance_claims insurance_claims_claim_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT insurance_claims_claim_reference_key UNIQUE (claim_reference);


--
-- Name: insurance_claims insurance_claims_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT insurance_claims_pkey PRIMARY KEY (id);


--
-- Name: insurance_members insurance_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_members
    ADD CONSTRAINT insurance_members_pkey PRIMARY KEY (id);


--
-- Name: insurance_payments insurance_payments_payment_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_payments
    ADD CONSTRAINT insurance_payments_payment_reference_key UNIQUE (payment_reference);


--
-- Name: insurance_payments insurance_payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_payments
    ADD CONSTRAINT insurance_payments_pkey PRIMARY KEY (id);


--
-- Name: insurance_schemes insurance_schemes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_schemes
    ADD CONSTRAINT insurance_schemes_pkey PRIMARY KEY (id);


--
-- Name: insurers insurers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurers
    ADD CONSTRAINT insurers_pkey PRIMARY KEY (id);


--
-- Name: invoice_history invoice_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_history
    ADD CONSTRAINT invoice_history_pkey PRIMARY KEY (id);


--
-- Name: kra_code_list kra_code_list_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_code_list
    ADD CONSTRAINT kra_code_list_pkey PRIMARY KEY (id);


--
-- Name: kra_county_code kra_county_code_county_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_county_code
    ADD CONSTRAINT kra_county_code_county_code_key UNIQUE (county_code);


--
-- Name: kra_county_code kra_county_code_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_county_code
    ADD CONSTRAINT kra_county_code_pkey PRIMARY KEY (id);


--
-- Name: kra_item_classification kra_item_classification_classification_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_item_classification
    ADD CONSTRAINT kra_item_classification_classification_code_key UNIQUE (classification_code);


--
-- Name: kra_item_classification kra_item_classification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_item_classification
    ADD CONSTRAINT kra_item_classification_pkey PRIMARY KEY (id);


--
-- Name: kra_notice kra_notice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_notice
    ADD CONSTRAINT kra_notice_pkey PRIMARY KEY (id);


--
-- Name: kra_packaging_type kra_packaging_type_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_packaging_type
    ADD CONSTRAINT kra_packaging_type_code_key UNIQUE (code);


--
-- Name: kra_packaging_type kra_packaging_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_packaging_type
    ADD CONSTRAINT kra_packaging_type_pkey PRIMARY KEY (id);


--
-- Name: kra_unit_of_measure kra_unit_of_measure_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_unit_of_measure
    ADD CONSTRAINT kra_unit_of_measure_code_key UNIQUE (code);


--
-- Name: kra_unit_of_measure kra_unit_of_measure_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kra_unit_of_measure
    ADD CONSTRAINT kra_unit_of_measure_pkey PRIMARY KEY (id);


--
-- Name: login_history login_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_history
    ADD CONSTRAINT login_history_pkey PRIMARY KEY (id);


--
-- Name: manufacturer manufacturer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manufacturer
    ADD CONSTRAINT manufacturer_pkey PRIMARY KEY (id);


--
-- Name: medicine_batches medicine_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine_batches
    ADD CONSTRAINT medicine_batches_pkey PRIMARY KEY (id);


--
-- Name: medicine_categories medicine_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine_categories
    ADD CONSTRAINT medicine_categories_pkey PRIMARY KEY (id);


--
-- Name: medicine medicine_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT medicine_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: outbox_events outbox_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_permission_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_permission_name_key UNIQUE (permission_name);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: pharmacy pharmacy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pharmacy
    ADD CONSTRAINT pharmacy_pkey PRIMARY KEY (id);


--
-- Name: prescription_items prescription_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescription_items
    ADD CONSTRAINT prescription_items_pkey PRIMARY KEY (id);


--
-- Name: prescriptions prescriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT prescriptions_pkey PRIMARY KEY (id);


--
-- Name: price_history price_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT price_history_pkey PRIMARY KEY (id);


--
-- Name: purchase_order_items purchase_order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_order_items
    ADD CONSTRAINT purchase_order_items_pkey PRIMARY KEY (id);


--
-- Name: purchase_orders purchase_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT purchase_orders_pkey PRIMARY KEY (id);


--
-- Name: receipts_compliance receipts_compliance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts_compliance
    ADD CONSTRAINT receipts_compliance_pkey PRIMARY KEY (id);


--
-- Name: receipts_compliance receipts_compliance_receipt_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts_compliance
    ADD CONSTRAINT receipts_compliance_receipt_number_key UNIQUE (receipt_number);


--
-- Name: receipts receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_pkey PRIMARY KEY (id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);


--
-- Name: sale_return_items sale_return_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_return_items
    ADD CONSTRAINT sale_return_items_pkey PRIMARY KEY (id);


--
-- Name: sale_returns sale_returns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_returns
    ADD CONSTRAINT sale_returns_pkey PRIMARY KEY (id);


--
-- Name: sales_items sales_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales_items
    ADD CONSTRAINT sales_items_pkey PRIMARY KEY (id);


--
-- Name: sales sales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_pkey PRIMARY KEY (id);


--
-- Name: sales sales_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_uuid_key UNIQUE (uuid);


--
-- Name: staff_shifts staff_shifts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_shifts
    ADD CONSTRAINT staff_shifts_pkey PRIMARY KEY (id);


--
-- Name: stock_movements stock_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT stock_movements_pkey PRIMARY KEY (id);


--
-- Name: stock stock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock
    ADD CONSTRAINT stock_pkey PRIMARY KEY (id);


--
-- Name: supplier_catalog_items supplier_catalog_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_catalog_items
    ADD CONSTRAINT supplier_catalog_items_pkey PRIMARY KEY (id);


--
-- Name: supplier_catalogs supplier_catalogs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_catalogs
    ADD CONSTRAINT supplier_catalogs_pkey PRIMARY KEY (id);


--
-- Name: supplier_invoices supplier_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_invoices
    ADD CONSTRAINT supplier_invoices_pkey PRIMARY KEY (id);


--
-- Name: supplier_payment supplier_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_payment
    ADD CONSTRAINT supplier_payment_pkey PRIMARY KEY (id);


--
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- Name: sync_outbox sync_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_outbox
    ADD CONSTRAINT sync_outbox_pkey PRIMARY KEY (event_id);


--
-- Name: sync_state sync_state_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_state
    ADD CONSTRAINT sync_state_pkey PRIMARY KEY (id);


--
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);


--
-- Name: system_settings system_settings_setting_key_branch_id_pharmacy_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_setting_key_branch_id_pharmacy_id_key UNIQUE (setting_key, branch_id, pharmacy_id);


--
-- Name: tax_category tax_category_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_category
    ADD CONSTRAINT tax_category_code_key UNIQUE (code);


--
-- Name: tax_category tax_category_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_category
    ADD CONSTRAINT tax_category_pkey PRIMARY KEY (id);


--
-- Name: tax_invoice_items tax_invoice_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice_items
    ADD CONSTRAINT tax_invoice_items_pkey PRIMARY KEY (id);


--
-- Name: tax_invoices tax_invoices_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoices
    ADD CONSTRAINT tax_invoices_invoice_number_key UNIQUE (invoice_number);


--
-- Name: tax_invoices tax_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoices
    ADD CONSTRAINT tax_invoices_pkey PRIMARY KEY (id);


--
-- Name: tax_invoices tax_invoices_sale_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoices
    ADD CONSTRAINT tax_invoices_sale_id_key UNIQUE (sale_id);


--
-- Name: tax_periods tax_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_periods
    ADD CONSTRAINT tax_periods_pkey PRIMARY KEY (id);


--
-- Name: terminal_configuration terminal_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_configuration
    ADD CONSTRAINT terminal_configuration_pkey PRIMARY KEY (id);


--
-- Name: terminal_heartbeats terminal_heartbeats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_heartbeats
    ADD CONSTRAINT terminal_heartbeats_pkey PRIMARY KEY (id);


--
-- Name: terminal_registry terminal_registry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_registry
    ADD CONSTRAINT terminal_registry_pkey PRIMARY KEY (id);


--
-- Name: terminal_registry terminal_registry_terminal_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_registry
    ADD CONSTRAINT terminal_registry_terminal_id_key UNIQUE (terminal_id);


--
-- Name: terminal_sessions terminal_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_sessions
    ADD CONSTRAINT terminal_sessions_pkey PRIMARY KEY (id);


--
-- Name: terminal_sessions terminal_sessions_session_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_sessions
    ADD CONSTRAINT terminal_sessions_session_id_key UNIQUE (session_id);


--
-- Name: terminals terminals_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminals
    ADD CONSTRAINT terminals_name_key UNIQUE (name);


--
-- Name: terminals terminals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminals
    ADD CONSTRAINT terminals_pkey PRIMARY KEY (terminal_id);


--
-- Name: transmission_attempts transmission_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transmission_attempts
    ADD CONSTRAINT transmission_attempts_pkey PRIMARY KEY (id);


--
-- Name: transmissions transmissions_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transmissions
    ADD CONSTRAINT transmissions_idempotency_key_key UNIQUE (idempotency_key);


--
-- Name: transmissions transmissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transmissions
    ADD CONSTRAINT transmissions_pkey PRIMARY KEY (id);


--
-- Name: document_sequences uk_doc_type_branch_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_sequences
    ADD CONSTRAINT uk_doc_type_branch_date UNIQUE (document_type, branch_code, sequence_date);


--
-- Name: terminal_registry uk_terminal_api_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_registry
    ADD CONSTRAINT uk_terminal_api_key UNIQUE (api_key);


--
-- Name: terminal_configuration uk_terminal_config_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_configuration
    ADD CONSTRAINT uk_terminal_config_key UNIQUE (terminal_id, config_key);


--
-- Name: terminal_registry uk_terminal_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_registry
    ADD CONSTRAINT uk_terminal_name UNIQUE (name);


--
-- Name: unit_of_measure unit_of_measure_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_of_measure
    ADD CONSTRAINT unit_of_measure_pkey PRIMARY KEY (id);


--
-- Name: user_branch_role user_branch_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_branch_role
    ADD CONSTRAINT user_branch_role_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_role_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_name_key UNIQUE (role_name);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_ce_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ce_created ON public.compliance_events USING btree (created_at);


--
-- Name: idx_ce_invoice; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ce_invoice ON public.compliance_events USING btree (invoice_id);


--
-- Name: idx_ce_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ce_type ON public.compliance_events USING btree (event_type);


--
-- Name: idx_heartbeat_terminal_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_heartbeat_terminal_time ON public.terminal_heartbeats USING btree (terminal_id, "timestamp");


--
-- Name: idx_sync_outbox_aggregate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_outbox_aggregate ON public.sync_outbox USING btree (aggregate_type, aggregate_id);


--
-- Name: idx_sync_outbox_retry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_outbox_retry ON public.sync_outbox USING btree (status, next_retry_at);


--
-- Name: idx_sync_outbox_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_outbox_status ON public.sync_outbox USING btree (status);


--
-- Name: login_history fk20v0mimmdegh2afs39uixlxpm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.login_history
    ADD CONSTRAINT fk20v0mimmdegh2afs39uixlxpm FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: prescription_items fk2lrk2206llk0851bq8moswdm3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescription_items
    ADD CONSTRAINT fk2lrk2206llk0851bq8moswdm3 FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: insurance_payments fk2oiewdsju6jp1ffkhr3bc4yin; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_payments
    ADD CONSTRAINT fk2oiewdsju6jp1ffkhr3bc4yin FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: sales fk2yh2bvoc5a4ptcgit3jcga45j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fk2yh2bvoc5a4ptcgit3jcga45j FOREIGN KEY (idempotency_id) REFERENCES public.idempotency(id);


--
-- Name: price_history fk3mitqxqn5l03bm4hc3kbiivuf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT fk3mitqxqn5l03bm4hc3kbiivuf FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: medicine fk40biskqqtrgm3j3hu2mddfcw5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT fk40biskqqtrgm3j3hu2mddfcw5 FOREIGN KEY (dosage_form_id) REFERENCES public.dosage_form(id);


--
-- Name: insurance_claims fk4ecbhwq04d7wtixtnou7ffv0v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fk4ecbhwq04d7wtixtnou7ffv0v FOREIGN KEY (member_id) REFERENCES public.insurance_members(id);


--
-- Name: terminal_heartbeats fk51xskdu815qgr1wl7homwmd0g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_heartbeats
    ADD CONSTRAINT fk51xskdu815qgr1wl7homwmd0g FOREIGN KEY (terminal_id) REFERENCES public.terminal_registry(id);


--
-- Name: sale_return_items fk525b5l8fv1m7rb4o92hy15n8n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_return_items
    ADD CONSTRAINT fk525b5l8fv1m7rb4o92hy15n8n FOREIGN KEY (sale_items_id) REFERENCES public.sales_items(id);


--
-- Name: grn_lines fk53hriqu0r5jirw3xo9k5v6qg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grn_lines
    ADD CONSTRAINT fk53hriqu0r5jirw3xo9k5v6qg FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: claim_batches fk54hav4m7xfjmtkeq9hvrf1sc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_batches
    ADD CONSTRAINT fk54hav4m7xfjmtkeq9hvrf1sc FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: sales fk5bgaw8g0rrbqdvafq36g58smk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fk5bgaw8g0rrbqdvafq36g58smk FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: system_settings fk5l38bwhydplecib303qqld7bg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT fk5l38bwhydplecib303qqld7bg FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);


--
-- Name: insurance_claims fk5stv8r6u3sjw8vkgyi6f6w1ol; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fk5stv8r6u3sjw8vkgyi6f6w1ol FOREIGN KEY (authorization_id) REFERENCES public.insurance_authorizations(id);


--
-- Name: medicine fk5tsh6p0q7v1sdyim714okhq13; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT fk5tsh6p0q7v1sdyim714okhq13 FOREIGN KEY (medicine_categories_id) REFERENCES public.medicine_categories(id);


--
-- Name: sales_items fk5yv46x9e53rv9p2wwxnyunm88; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales_items
    ADD CONSTRAINT fk5yv46x9e53rv9p2wwxnyunm88 FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: staff_shifts fk6iegeykibinxwee5mvvmy9h5w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_shifts
    ADD CONSTRAINT fk6iegeykibinxwee5mvvmy9h5w FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: system_settings fk6j7i6v0dsi7a5wtygupqv90v7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT fk6j7i6v0dsi7a5wtygupqv90v7 FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: expenses fk6pivs51nq6irxhnvmxhf18v9r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fk6pivs51nq6irxhnvmxhf18v9r FOREIGN KEY (expense_category_id) REFERENCES public.expense_category(id);


--
-- Name: prescription_items fk6uh7tdy2lv6sx34u1365acqsf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescription_items
    ADD CONSTRAINT fk6uh7tdy2lv6sx34u1365acqsf FOREIGN KEY (prescription_id) REFERENCES public.prescriptions(id);


--
-- Name: sales fk72ep16wuoj7nllumicmk2ie3s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fk72ep16wuoj7nllumicmk2ie3s FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- Name: sale_return_items fk769oilok78ocvrpcx13qi7mte; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_return_items
    ADD CONSTRAINT fk769oilok78ocvrpcx13qi7mte FOREIGN KEY (sale_returns_id) REFERENCES public.sale_returns(id);


--
-- Name: insurance_claims fk8816s7kqfwdgrcethuoyqs264; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fk8816s7kqfwdgrcethuoyqs264 FOREIGN KEY (payment_id) REFERENCES public.insurance_payments(id);


--
-- Name: cash_drawers fk8i02k4gcewqif0sw49hya8meg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_drawers
    ADD CONSTRAINT fk8i02k4gcewqif0sw49hya8meg FOREIGN KEY (staff_shifts_id) REFERENCES public.staff_shifts(id);


--
-- Name: terminal_configuration fk8ujlkxgsy4of78rdek6werkot; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_configuration
    ADD CONSTRAINT fk8ujlkxgsy4of78rdek6werkot FOREIGN KEY (terminal_id) REFERENCES public.terminal_registry(id);


--
-- Name: stock fk8wkpxnja0ikk6t0xp3ju8aoar; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock
    ADD CONSTRAINT fk8wkpxnja0ikk6t0xp3ju8aoar FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: insurance_schemes fk95srk7nblotg891btse36u68p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_schemes
    ADD CONSTRAINT fk95srk7nblotg891btse36u68p FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: tax_invoice_items fk9daffbejedtc0x6k4q3rcruh5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_invoice_items
    ADD CONSTRAINT fk9daffbejedtc0x6k4q3rcruh5 FOREIGN KEY (tax_invoice_id) REFERENCES public.tax_invoices(id);


--
-- Name: medicine_batches fk9nxacec42bbmbas6clx9crif8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine_batches
    ADD CONSTRAINT fk9nxacec42bbmbas6clx9crif8 FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: hardware_peripherals fka1ld45320wwj45tb7g7x8gs63; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hardware_peripherals
    ADD CONSTRAINT fka1ld45320wwj45tb7g7x8gs63 FOREIGN KEY (terminal_id) REFERENCES public.terminal_registry(id);


--
-- Name: sale_return_items fka4ywxh152acr06cbpti6lc6ul; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_return_items
    ADD CONSTRAINT fka4ywxh152acr06cbpti6lc6ul FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: receipts fka9i6fjl1s80c1iiosdira85wn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT fka9i6fjl1s80c1iiosdira85wn FOREIGN KEY (sales_id) REFERENCES public.sales(id);


--
-- Name: dispensed_items fkac4jmkkj3kjh6y1431bs7luha; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispensed_items
    ADD CONSTRAINT fkac4jmkkj3kjh6y1431bs7luha FOREIGN KEY (prescription_items_id) REFERENCES public.prescription_items(id);


--
-- Name: sales_items fkaoo9khjn0pkmkrxxvoi3mmfrx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales_items
    ADD CONSTRAINT fkaoo9khjn0pkmkrxxvoi3mmfrx FOREIGN KEY (sales_id) REFERENCES public.sales(id);


--
-- Name: purchase_order_items fkb37a6x7vn0f0u2ffyfn3e472y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_order_items
    ADD CONSTRAINT fkb37a6x7vn0f0u2ffyfn3e472y FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: claim_reconciliations fkbduq0xmefonl7rb928vi3ceqq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_reconciliations
    ADD CONSTRAINT fkbduq0xmefonl7rb928vi3ceqq FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: goods_received_notes fkbehrti09d8d5q3smgo8ww32ps; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_received_notes
    ADD CONSTRAINT fkbehrti09d8d5q3smgo8ww32ps FOREIGN KEY (received_by_user_id) REFERENCES public.users(id);


--
-- Name: staff_shifts fkbosqsjb7ikx5eeftsew2gube6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_shifts
    ADD CONSTRAINT fkbosqsjb7ikx5eeftsew2gube6 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: supplier_invoices fkc4mhibggokg7879l6xdc996rq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_invoices
    ADD CONSTRAINT fkc4mhibggokg7879l6xdc996rq FOREIGN KEY (suppliers_id) REFERENCES public.suppliers(id);


--
-- Name: dispensed_items fkc8focs264mitchy45csvfsdb7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispensed_items
    ADD CONSTRAINT fkc8focs264mitchy45csvfsdb7 FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: sale_returns fkchqro8hipndq784urrg5c5bor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_returns
    ADD CONSTRAINT fkchqro8hipndq784urrg5c5bor FOREIGN KEY (sales_id) REFERENCES public.sales(id);


--
-- Name: insurance_claims fkclrhqt9tmcnl789lojvatdvo8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fkclrhqt9tmcnl789lojvatdvo8 FOREIGN KEY (batch_id) REFERENCES public.claim_batches(id);


--
-- Name: insurance_authorizations fkcy18mioo0g00vr277894qgaea; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_authorizations
    ADD CONSTRAINT fkcy18mioo0g00vr277894qgaea FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: grn_lines fkd7dl28cmvt7j8rn2coik0knw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grn_lines
    ADD CONSTRAINT fkd7dl28cmvt7j8rn2coik0knw FOREIGN KEY (grn_id) REFERENCES public.goods_received_notes(id);


--
-- Name: cash_transactions fkdjntvoscony83753ooewggwru; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_transactions
    ADD CONSTRAINT fkdjntvoscony83753ooewggwru FOREIGN KEY (cash_drawers_id) REFERENCES public.cash_drawers(id);


--
-- Name: expiry fkdtwdy6349wkhoucng283jhxuh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expiry
    ADD CONSTRAINT fkdtwdy6349wkhoucng283jhxuh FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: medicine fke31yejutctvbu6mwa0ntaww5d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT fke31yejutctvbu6mwa0ntaww5d FOREIGN KEY (tax_category_id) REFERENCES public.tax_category(id);


--
-- Name: compliance_batch_items fke6pndaanmu531ppwx2ve8fg7k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_batch_items
    ADD CONSTRAINT fke6pndaanmu531ppwx2ve8fg7k FOREIGN KEY (batch_id) REFERENCES public.compliance_batches(id);


--
-- Name: role_permissions fkegdk29eiy7mdtefy5c7eirr6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: user_branch_role fkeiadjk0wgivbpbhxsru5w7pcg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_branch_role
    ADD CONSTRAINT fkeiadjk0wgivbpbhxsru5w7pcg FOREIGN KEY (assigned_by) REFERENCES public.users(id);


--
-- Name: stock_movements fkfqq1iu0gt0la6ruk2o62bry5v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fkfqq1iu0gt0la6ruk2o62bry5v FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: expenses fkfquq3h9u0rhw7vcwariiscnsi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fkfquq3h9u0rhw7vcwariiscnsi FOREIGN KEY (cash_drawers_id) REFERENCES public.cash_drawers(id);


--
-- Name: grn_lines fkgnumuuk94uf61uidl7r6lbdm9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grn_lines
    ADD CONSTRAINT fkgnumuuk94uf61uidl7r6lbdm9 FOREIGN KEY (batch_id) REFERENCES public.medicine_batches(id);


--
-- Name: purchase_order_items fkgok15gx5uafv8ea0bmwokgu28; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_order_items
    ADD CONSTRAINT fkgok15gx5uafv8ea0bmwokgu28 FOREIGN KEY (purchase_orders_id) REFERENCES public.purchase_orders(id);


--
-- Name: price_history fkgvwq681eg9w3xlm018336vusi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT fkgvwq681eg9w3xlm018336vusi FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: medicine fkh4prisogtynkdjgitqyiwsp85; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT fkh4prisogtynkdjgitqyiwsp85 FOREIGN KEY (manufacturer_id) REFERENCES public.manufacturer(id);


--
-- Name: role_permissions fkhgc8c8og9nyu47lqmbgmg84jq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkhgc8c8og9nyu47lqmbgmg84jq FOREIGN KEY (role_id) REFERENCES public.user_roles(id);


--
-- Name: expenses fkhpk0n2cbnfiuu5nrgl0ika3hq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fkhpk0n2cbnfiuu5nrgl0ika3hq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: controlled_drugs fkhqtt1y4373xqhdga3fd6o09v0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.controlled_drugs
    ADD CONSTRAINT fkhqtt1y4373xqhdga3fd6o09v0 FOREIGN KEY (prescriptions_id) REFERENCES public.prescriptions(id);


--
-- Name: price_history fki2e1ecgjvnsda00ixqtxvydla; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT fki2e1ecgjvnsda00ixqtxvydla FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: purchase_orders fki5dg8p8me0c7hksirtqba2td5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT fki5dg8p8me0c7hksirtqba2td5 FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: sales fkibkujij7bt0a6w0qwf971jhsc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fkibkujij7bt0a6w0qwf971jhsc FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: controlled_drugs fkidq3v0aaya3rnc2l7w90q1gm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.controlled_drugs
    ADD CONSTRAINT fkidq3v0aaya3rnc2l7w90q1gm FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: users fkixo09sv3j1j6hfox3cx6d2ggg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkixo09sv3j1j6hfox3cx6d2ggg FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: purchase_orders fkjbhnbrdrwujql73ff2rk5rrii; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT fkjbhnbrdrwujql73ff2rk5rrii FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: goods_received_notes fkjfoyx3ohkkbmll7g3it65445y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_received_notes
    ADD CONSTRAINT fkjfoyx3ohkkbmll7g3it65445y FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- Name: expiry fkjmskrbf6h3ki9ekth6sc20lt2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expiry
    ADD CONSTRAINT fkjmskrbf6h3ki9ekth6sc20lt2 FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: supplier_payment fkjpnr3fphj8t97y5p384yhvoh5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_payment
    ADD CONSTRAINT fkjpnr3fphj8t97y5p384yhvoh5 FOREIGN KEY (supplier_invoices_id) REFERENCES public.supplier_invoices(id);


--
-- Name: audit_logs fkjs4iimve3y0xssbtve5ysyef0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkjs4iimve3y0xssbtve5ysyef0 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: dispensed_items fkkuvpypul2ws12aw8ehoki24dr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispensed_items
    ADD CONSTRAINT fkkuvpypul2ws12aw8ehoki24dr FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: stock fklef7qxrn4u7o7q0n813eg9c6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock
    ADD CONSTRAINT fklef7qxrn4u7o7q0n813eg9c6e FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: invoice_history fkma6p0qe429nxuw9w96y244pk1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_history
    ADD CONSTRAINT fkma6p0qe429nxuw9w96y244pk1 FOREIGN KEY (invoice_id) REFERENCES public.tax_invoices(id);


--
-- Name: insurance_claims fkme5peau7140m36qibyr3ymobt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fkme5peau7140m36qibyr3ymobt FOREIGN KEY (scheme_id) REFERENCES public.insurance_schemes(id);


--
-- Name: purchase_orders fknlnqdpksjdxiec8vnpteoax74; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT fknlnqdpksjdxiec8vnpteoax74 FOREIGN KEY (suppliers_id) REFERENCES public.suppliers(id);


--
-- Name: supplier_payment fknp959x8ujnjqc81qkg9p9dxkc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_payment
    ADD CONSTRAINT fknp959x8ujnjqc81qkg9p9dxkc FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: stock_movements fknwhjcjh4siqc7vsa3k43318jh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fknwhjcjh4siqc7vsa3k43318jh FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: sale_returns fko1brvqt2p5v2fq6jk8jd6b3v4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_returns
    ADD CONSTRAINT fko1brvqt2p5v2fq6jk8jd6b3v4 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: goods_received_notes fkof353e8738aslm71mu65tkn0o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.goods_received_notes
    ADD CONSTRAINT fkof353e8738aslm71mu65tkn0o FOREIGN KEY (purchase_orders_id) REFERENCES public.purchase_orders(id);


--
-- Name: user_branch_role fkp4m1uf4rachb1w5sdo41joier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_branch_role
    ADD CONSTRAINT fkp4m1uf4rachb1w5sdo41joier FOREIGN KEY (branch_id) REFERENCES public.branch(id);


--
-- Name: controlled_drugs fkpfn5q7qtemg6ya2bops5l5k75; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.controlled_drugs
    ADD CONSTRAINT fkpfn5q7qtemg6ya2bops5l5k75 FOREIGN KEY (medicine_id) REFERENCES public.medicine(id);


--
-- Name: terminal_sessions fkpo7qjqhprl7d82n1gmmn1sb7o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terminal_sessions
    ADD CONSTRAINT fkpo7qjqhprl7d82n1gmmn1sb7o FOREIGN KEY (terminal_id) REFERENCES public.terminal_registry(id);


--
-- Name: medicine fkpu9rpg0gvf96qunkfen9aaq3e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medicine
    ADD CONSTRAINT fkpu9rpg0gvf96qunkfen9aaq3e FOREIGN KEY (unit_of_measure_id) REFERENCES public.unit_of_measure(id);


--
-- Name: branch fkq5h5i3k0jj9hoohtuhu17jh84; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT fkq5h5i3k0jj9hoohtuhu17jh84 FOREIGN KEY (pharmacy_id) REFERENCES public.pharmacy(id);


--
-- Name: purchase_orders fkquq1njrq27p9ye5u19f1yvnfp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT fkquq1njrq27p9ye5u19f1yvnfp FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: stock_movements fkr90m15ux2xnbjsvpv6rb3la5q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fkr90m15ux2xnbjsvpv6rb3la5q FOREIGN KEY (medicine_batches_id) REFERENCES public.medicine_batches(id);


--
-- Name: staff_shifts fkrkfsvnhsvjcjpukvjptgk3q1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.staff_shifts
    ADD CONSTRAINT fkrkfsvnhsvjcjpukvjptgk3q1 FOREIGN KEY (role_id) REFERENCES public.user_roles(id);


--
-- Name: user_branch_role fkrmfnu4e6gwoj3c568f181ulrs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_branch_role
    ADD CONSTRAINT fkrmfnu4e6gwoj3c568f181ulrs FOREIGN KEY (user_roles_id) REFERENCES public.user_roles(id);


--
-- Name: payments fkrn16shajc8b1sihgq2moa5dx3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkrn16shajc8b1sihgq2moa5dx3 FOREIGN KEY (sales_id) REFERENCES public.sales(id);


--
-- Name: supplier_catalog_items fkruy082wntt9wd8ncaylkh73iq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_catalog_items
    ADD CONSTRAINT fkruy082wntt9wd8ncaylkh73iq FOREIGN KEY (catalog_id) REFERENCES public.supplier_catalogs(id);


--
-- Name: user_branch_role fks25ks2cmti9cxahrdajevha6i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_branch_role
    ADD CONSTRAINT fks25ks2cmti9cxahrdajevha6i FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: claim_attachments fksq4t12o5acr9xy62690r7ykc6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_attachments
    ADD CONSTRAINT fksq4t12o5acr9xy62690r7ykc6 FOREIGN KEY (claim_id) REFERENCES public.insurance_claims(id);


--
-- Name: insurance_members fktp1qhgopgjoafg7uperv9blo6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_members
    ADD CONSTRAINT fktp1qhgopgjoafg7uperv9blo6 FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: insurance_claims fktq9vt1cqh62mpoxyv7atilhdl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insurance_claims
    ADD CONSTRAINT fktq9vt1cqh62mpoxyv7atilhdl FOREIGN KEY (insurer_id) REFERENCES public.insurers(id);


--
-- Name: transmission_attempts fkxg9oefdn39oq47wxdfn1fo2f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transmission_attempts
    ADD CONSTRAINT fkxg9oefdn39oq47wxdfn1fo2f FOREIGN KEY (transmission_id) REFERENCES public.transmissions(id);


--
-- PostgreSQL database dump complete
--
