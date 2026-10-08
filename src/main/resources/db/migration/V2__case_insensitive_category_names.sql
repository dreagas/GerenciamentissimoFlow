ALTER TABLE category DROP CONSTRAINT uq_category_name;

CREATE UNIQUE INDEX uq_category_name_ci ON category (LOWER(name));

ALTER TABLE product
    ADD CONSTRAINT ck_product_name_not_blank CHECK (length(trim(name)) > 0),
    ADD CONSTRAINT ck_product_sku_not_blank CHECK (length(trim(sku)) > 0);
