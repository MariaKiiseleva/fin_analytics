CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    tax_percent     NUMERIC(7,4) NOT NULL DEFAULT 0.0000,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    role            VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_tax_percent CHECK (tax_percent >= 0 AND tax_percent <= 100)
);
