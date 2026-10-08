CREATE TABLE order_confirmation_idempotency (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    idempotency_key VARCHAR(128) NOT NULL,
    order_id UUID NOT NULL REFERENCES customer_order(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_confirmation_key UNIQUE (actor_id, idempotency_key)
);
CREATE INDEX ix_order_confirmation_expires_at ON order_confirmation_idempotency(expires_at);
