CREATE TABLE marketplace_credentials (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE REFERENCES users(id),
    provider          VARCHAR(30) NOT NULL DEFAULT 'WILDBERRIES',
    encrypted_token   TEXT NOT NULL,
    token_mask        VARCHAR(30),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
