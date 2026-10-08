CREATE TABLE supplier (
    id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    document VARCHAR(40),
    email VARCHAR(320),
    phone VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_supplier_name_not_blank CHECK (length(trim(name)) > 0)
);
