--liquibase formatted sql
--changeset codex:006-outbox-trace-context
ALTER TABLE local_outbox
    ADD COLUMN traceparent VARCHAR(55)
    CHECK (traceparent IS NULL OR traceparent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$');
