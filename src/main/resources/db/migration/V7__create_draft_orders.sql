CREATE TABLE customer_order (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    warehouse_id UUID NOT NULL REFERENCES warehouse(id),
    created_by_id UUID NOT NULL REFERENCES app_user(id),
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_customer_order_status CHECK (status IN ('DRAFT')),
    CONSTRAINT ck_customer_order_total CHECK (total_amount >= 0)
);
CREATE INDEX ix_customer_order_created_by ON customer_order(created_by_id);
CREATE INDEX ix_customer_order_created_at ON customer_order(created_at DESC);

CREATE TABLE order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES product(id),
    quantity BIGINT NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    line_total NUMERIC(19, 4) NOT NULL,
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_item_line_total CHECK (line_total >= 0)
);
CREATE INDEX ix_order_item_order_id ON order_item(order_id);
