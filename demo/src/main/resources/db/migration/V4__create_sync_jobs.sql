CREATE TABLE sync_jobs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    date_from           DATE NOT NULL,
    date_to             DATE NOT NULL,
    status              VARCHAR(40) NOT NULL,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    received_rows       INTEGER NOT NULL DEFAULT 0,
    inserted_rows       INTEGER NOT NULL DEFAULT 0,
    updated_rows        INTEGER NOT NULL DEFAULT 0,
    duplicate_rows      INTEGER NOT NULL DEFAULT 0,
    affected_days       INTEGER NOT NULL DEFAULT 0,
    unrecognized_rows   INTEGER NOT NULL DEFAULT 0,
    error_code          VARCHAR(100),
    error_message       TEXT,
    CONSTRAINT chk_sync_period CHECK (date_from <= date_to)
);

CREATE INDEX idx_sync_jobs_user_requested
    ON sync_jobs(user_id, requested_at DESC);

CREATE INDEX idx_sync_jobs_user_status
    ON sync_jobs(user_id, status);
