CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
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

INSERT INTO admin_users (id, username, password_hash)
VALUES (1, 'admin', '$2a$10$3GQJSmX2m7Rv2c30O5glsexTxAUcpyj5DpETnqohVxxRnQR9OJQm2')
ON CONFLICT (id) DO NOTHING;

INSERT INTO series (id, name, min_value, max_value, color, icon, unit) VALUES
(1, 'PM2.5', 0.0, 150.0, '#1f77b4', 'cloud', 'µg/m³'),
(2, 'PM10', 0.0, 250.0, '#ff7f0e', 'wind', 'µg/m³'),
(3, 'NO2', 0.0, 200.0, '#2ca02c', 'smog', 'µg/m³')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sensors (id, name, series_id, api_key_hash, created_at) VALUES
(1, 'PM2.5 Living Room', 1, '735eb536cdf143ca89efb5cf9658750bd77a771c2664a5e0b413f51e2e15f2e0', '2026-10-01T00:00:00Z'),
(2, 'PM10 Balcony', 2, '5ba0ef5e933f56130f405307aac4daa4764b55b056caf2778d32d34c0a4e675d', '2026-10-01T00:00:00Z'),
(3, 'NO2 Street Facing', 3, 'e1b213678adbbfbad87417a568584552cab106500efae64ae828b5edf0c12170', '2026-10-01T00:00:00Z')
ON CONFLICT (id) DO NOTHING;

SELECT setval('admin_users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM admin_users));
SELECT setval('series_id_seq', (SELECT COALESCE(MAX(id), 1) FROM series));
SELECT setval('sensors_id_seq', (SELECT COALESCE(MAX(id), 1) FROM sensors));
