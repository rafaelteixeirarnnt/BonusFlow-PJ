CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(120) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(80) NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    justification TEXT NOT NULL,
    performed_by_user_id BIGINT NOT NULL REFERENCES users(id),
    performed_by_user_name VARCHAR(255) NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address VARCHAR(80)
);

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX ix_audit_logs_action ON audit_logs (action);
CREATE INDEX ix_audit_logs_user ON audit_logs (performed_by_user_id);
CREATE INDEX ix_audit_logs_performed_at ON audit_logs (performed_at);
