--liquibase formatted sql

-- The initial schema already owns information_requests, assessment_started_at,
-- and last_information_requested_at. This changeset adds only missing evidence.
--changeset codex:002-claim-persistence
ALTER TABLE claims
    ADD COLUMN last_information_received_at TIMESTAMPTZ,
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN member_acknowledged_rejection_at TIMESTAMPTZ;

CREATE TABLE claim_audit (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL,
    sequence BIGINT NOT NULL CHECK (sequence >= 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    from_state VARCHAR(80),
    to_state VARCHAR(80) NOT NULL,
    actor_id VARCHAR(120) NOT NULL,
    role VARCHAR(80) NOT NULL,
    reason TEXT NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    workflow_version VARCHAR(80) NOT NULL,
    UNIQUE (claim_id, sequence)
);
CREATE INDEX claim_audit_claim_sequence_idx ON claim_audit (claim_id, sequence DESC, id);

CREATE TABLE system_operation_audit (
    id UUID PRIMARY KEY,
    action VARCHAR(120) NOT NULL,
    resource VARCHAR(120) NOT NULL,
    resource_id VARCHAR(160),
    actor_id VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    error_type VARCHAR(160),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX system_operation_audit_occurred_idx ON system_operation_audit (occurred_at DESC, id);

CREATE TABLE command_receipts (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(120) NOT NULL,
    scope VARCHAR(200) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    fingerprint CHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
    result_json JSONB NOT NULL CHECK (jsonb_typeof(result_json) = 'object'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (actor_id, scope, idempotency_key)
);

CREATE TABLE payment_requests (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL UNIQUE REFERENCES claims(id),
    request_id UUID NOT NULL UNIQUE,
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    amount NUMERIC(18,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED','CONFIRMED','FAILED')),
    reference VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    confirmed_at TIMESTAMPTZ,
    CHECK ((status = 'CONFIRMED') = (reference IS NOT NULL AND confirmed_at IS NOT NULL))
);

CREATE TABLE local_outbox (
    id UUID PRIMARY KEY,
    claim_id UUID REFERENCES claims(id),
    effect VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    delivered_at TIMESTAMPTZ,
    last_error VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX local_outbox_due_idx ON local_outbox (available_at, id) WHERE delivered_at IS NULL;

--changeset codex:003-audit-immutability-function splitStatements:false
CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit records are append-only' USING ERRCODE = '42501';
END;
$$

--changeset codex:004-claim-audit-immutability
CREATE TRIGGER claim_audit_immutable BEFORE UPDATE OR DELETE ON claim_audit
    FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

--changeset codex:005-system-audit-immutability
CREATE TRIGGER system_operation_audit_immutable BEFORE UPDATE OR DELETE ON system_operation_audit
    FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

-- These roles are provisioned by docker/initdb before Liquibase starts. The
-- migration login is the schema owner; the runtime login receives only DML.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO claims_runtime;
GRANT SELECT, INSERT ON claims TO claims_runtime;
GRANT UPDATE (state, facts, information_requests, last_information_requested_at,
    last_information_received_at, assessment_started_at, rejected_at,
    member_acknowledged_rejection_at, updated_at, version) ON claims TO claims_runtime;
GRANT SELECT, INSERT ON claim_audit, system_operation_audit TO claims_runtime;
GRANT SELECT, INSERT ON command_receipts TO claims_runtime;
GRANT SELECT, INSERT ON payment_requests TO claims_runtime;
GRANT UPDATE (status, reference, confirmed_at) ON payment_requests TO claims_runtime;
GRANT SELECT, INSERT ON local_outbox TO claims_runtime;
GRANT UPDATE (attempts, available_at, delivered_at, last_error) ON local_outbox TO claims_runtime;
