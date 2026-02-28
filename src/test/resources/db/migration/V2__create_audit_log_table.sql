-- V2__create_audit_log_table.sql
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       VARCHAR(50) NOT NULL,
    action          VARCHAR(20) NOT NULL,
    changed_by      VARCHAR(50),
    changed_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    session_id      VARCHAR(100)
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_changed_at ON audit_log(changed_at);