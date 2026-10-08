CREATE TABLE inventory_movement (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id),
    warehouse_id UUID NOT NULL REFERENCES warehouse(id),
    type VARCHAR(30) NOT NULL,
    quantity BIGINT NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_movement_quantity_positive CHECK (quantity > 0)
);
