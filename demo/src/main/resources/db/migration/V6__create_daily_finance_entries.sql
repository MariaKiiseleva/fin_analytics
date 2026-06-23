CREATE TABLE daily_finance_entries (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL REFERENCES users(id),
    business_date                   DATE NOT NULL,
    nm_id                           BIGINT,
    product_name                    VARCHAR(255),

    sales_quantity                  INTEGER NOT NULL DEFAULT 0,
    return_quantity                 INTEGER NOT NULL DEFAULT 0,
    net_quantity                    INTEGER NOT NULL DEFAULT 0,

    sales_amount                    NUMERIC(19,2) NOT NULL DEFAULT 0,
    returns_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    net_revenue_amount              NUMERIC(19,2) NOT NULL DEFAULT 0,

    commission_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    logistics_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,

    cost_amount                     NUMERIC(19,2) NOT NULL DEFAULT 0,
    tax_amount                      NUMERIC(19,2) NOT NULL DEFAULT 0,
    product_profit_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,

    acquiring_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    storage_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    acceptance_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    penalty_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    additional_deductions_amount    NUMERIC(19,2) NOT NULL DEFAULT 0,

    has_cost                        BOOLEAN NOT NULL DEFAULT TRUE,
    calculation_version             INTEGER NOT NULL,
    calculated_at                   TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_daily_row_shape CHECK (
        (nm_id IS NOT NULL)
        OR
        (
            nm_id IS NULL
            AND sales_quantity = 0
            AND return_quantity = 0
            AND net_quantity = 0
            AND sales_amount = 0
            AND returns_amount = 0
            AND net_revenue_amount = 0
            AND commission_amount = 0
            AND logistics_amount = 0
            AND cost_amount = 0
            AND tax_amount = 0
            AND product_profit_amount = 0
        )
    )
);

CREATE UNIQUE INDEX uq_daily_product
    ON daily_finance_entries(user_id, business_date, nm_id)
    WHERE nm_id IS NOT NULL;

CREATE UNIQUE INDEX uq_daily_common
    ON daily_finance_entries(user_id, business_date)
    WHERE nm_id IS NULL;

CREATE INDEX idx_daily_report_period
    ON daily_finance_entries(user_id, business_date);
