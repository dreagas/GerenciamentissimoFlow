CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(320),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_audit_event_occurred_at ON audit_event(occurred_at);
CREATE INDEX ix_audit_event_actor ON audit_event(actor_id);
