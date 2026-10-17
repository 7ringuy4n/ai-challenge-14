--liquibase formatted sql

--changeset codex:001-initial-claims-schema
CREATE TABLE claims (
    id UUID PRIMARY KEY,
    member_id VARCHAR(120) NOT NULL,
    state VARCHAR(80) NOT NULL,
    policy_limit NUMERIC(18, 2) NOT NULL CHECK (policy_limit >= 0),
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    facts JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(facts) = 'object'),
    information_requests INTEGER NOT NULL DEFAULT 0 CHECK (information_requests >= 0),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    assessment_started_at TIMESTAMPTZ,
    last_information_requested_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX claims_member_created_id_idx ON claims (member_id, created_at DESC, id);
