ALTER TABLE customer_order DROP CONSTRAINT ck_customer_order_status;
ALTER TABLE customer_order ADD CONSTRAINT ck_customer_order_status
    CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED', 'FULFILLED'));
ALTER TABLE customer_order ADD COLUMN fulfilled_at TIMESTAMPTZ;
ALTER TABLE customer_order ADD COLUMN fulfilled_by_id UUID REFERENCES app_user(id);
ALTER TABLE customer_order ADD CONSTRAINT ck_customer_order_fulfillment
    CHECK ((status = 'FULFILLED' AND fulfilled_at IS NOT NULL AND fulfilled_by_id IS NOT NULL)
        OR (status <> 'FULFILLED' AND fulfilled_at IS NULL AND fulfilled_by_id IS NULL));
