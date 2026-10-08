CREATE TABLE warehouse (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(240) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_warehouse_code UNIQUE (code),
    CONSTRAINT ck_warehouse_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_warehouse_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_warehouse_location_not_blank CHECK (length(trim(location)) > 0)
);
