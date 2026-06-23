CREATE TABLE financial_operations_raw (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL REFERENCES users(id),
    sync_job_id                 BIGINT NOT NULL REFERENCES sync_jobs(id),

    row_hash                    VARCHAR(64) NOT NULL,
    external_operation_id       VARCHAR(255),
    srid                        VARCHAR(255),
    nm_id                       BIGINT,

    supplier_oper_name          VARCHAR(255),
    document_type               VARCHAR(100),

    order_at                    TIMESTAMPTZ,
    sale_at                     TIMESTAMPTZ,
    report_at                   TIMESTAMPTZ,
    business_date               DATE NOT NULL,

    quantity                    INTEGER,
    retail_amount               NUMERIC(19,2),
    retail_amount_with_discount NUMERIC(19,2),
    seller_amount               NUMERIC(19,2),
    commission_amount           NUMERIC(19,2),
    logistics_amount            NUMERIC(19,2),
    rebill_logistics_amount     NUMERIC(19,2),
    pvz_reward_amount           NUMERIC(19,2),
    acquiring_amount            NUMERIC(19,2),
    storage_amount              NUMERIC(19,2),
    acceptance_amount           NUMERIC(19,2),
    penalty_amount              NUMERIC(19,2),
    deduction_amount            NUMERIC(19,2),

    classification_status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    classification_code         VARCHAR(60),
    raw_payload                 JSONB NOT NULL,

    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_raw_row_hash UNIQUE (user_id, row_hash)
);

CREATE INDEX idx_raw_user_date
    ON financial_operations_raw(user_id, business_date);

CREATE INDEX idx_raw_user_srid_date
    ON financial_operations_raw(user_id, srid, business_date);

CREATE INDEX idx_raw_user_nm_date
    ON financial_operations_raw(user_id, nm_id, business_date);
