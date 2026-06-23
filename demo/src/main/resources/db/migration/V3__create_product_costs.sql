CREATE TABLE product_costs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    nm_id           BIGINT NOT NULL,
    product_name    VARCHAR(255),
    valid_from      DATE NOT NULL,
    cost_amount     NUMERIC(19,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_product_cost_non_negative CHECK (cost_amount >= 0),
    CONSTRAINT uq_product_cost_date UNIQUE (user_id, nm_id, valid_from)
);

CREATE INDEX idx_product_cost_lookup
    ON product_costs(user_id, nm_id, valid_from DESC);
