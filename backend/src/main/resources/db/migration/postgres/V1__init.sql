CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS access_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES admin_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at VARCHAR(64) NOT NULL,
    created_at VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS series (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    color VARCHAR(32) NOT NULL,
    icon VARCHAR(64),
    unit VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS sensors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    series_id BIGINT NOT NULL REFERENCES series (id) ON DELETE CASCADE,
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS error_log (
    id BIGSERIAL PRIMARY KEY,
    created_at VARCHAR(64) NOT NULL,
    message TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sensors_series ON sensors (series_id);
CREATE INDEX IF NOT EXISTS idx_access_tokens_hash ON access_tokens (token_hash);
