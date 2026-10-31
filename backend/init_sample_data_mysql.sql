-- =====================================================================
-- ZAI 26Z - MySQL Sample Data Initialization Script
-- Populates 3 series with 16 measurements each (fulfills Requirement F11)
-- =====================================================================

-- 1. Ensure Tables Exist (compatible with MySQL and JPA schema)
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    min_value DOUBLE NOT NULL,
    max_value DOUBLE NOT NULL,
    color VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    unit VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sensors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    series_id BIGINT NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at VARCHAR(255) NOT NULL,
    FOREIGN KEY (series_id) REFERENCES series (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS measurements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    series_id BIGINT NOT NULL,
    sensor_id BIGINT,
    value DOUBLE NOT NULL,
    timestamp VARCHAR(255) NOT NULL,
    FOREIGN KEY (series_id) REFERENCES series (id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Administrator Account (username: admin / password: Admin123!)
INSERT INTO admin_users (id, username, password_hash)
VALUES (1, 'admin', '$2a$10$3GQJSmX2m7Rv2c30O5glsexTxAUcpyj5DpETnqohVxxRnQR9OJQm2')
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- 3. Measurement Series (PM2.5, PM10, NO2)
INSERT INTO series (id, name, min_value, max_value, color, icon, unit) VALUES
(1, 'PM2.5', 0.0, 150.0, '#1f77b4', 'cloud', 'µg/m³'),
(2, 'PM10', 0.0, 250.0, '#ff7f0e', 'wind', 'µg/m³'),
(3, 'NO2', 0.0, 200.0, '#2ca02c', 'smog', 'µg/m³')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 4. Sensors (Pre-configured API keys for testing)
-- PM2.5 Sensor Key: sensor-pm25-key-1
-- PM10  Sensor Key: sensor-pm10-key-2
-- NO2   Sensor Key: sensor-no2-key-3
INSERT INTO sensors (id, name, series_id, api_key_hash, created_at) VALUES
(1, 'PM2.5 Living Room', 1, '735eb536cdf143ca89efb5cf9658750bd77a771c2664a5e0b413f51e2e15f2e0', '2026-10-01T00:00:00Z'),
(2, 'PM10 Balcony', 2, '5ba0ef5e933f56130f405307aac4daa4764b55b056caf2778d32d34c0a4e675d', '2026-10-01T00:00:00Z'),
(3, 'NO2 Street Facing', 3, 'e1b213678adbbfbad87417a568584552cab106500efae64ae828b5edf0c12170', '2026-10-01T00:00:00Z')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 5. 16 Sample Measurements per Series (48 total, F11 Requirement)
INSERT INTO measurements (series_id, sensor_id, value, timestamp) VALUES
(1, 1, 50.0, '2026-10-26T06:00:00Z'),
(1, 1, 59.8, '2026-10-26T07:00:00Z'),
(1, 1, 68.6, '2026-10-26T08:00:00Z'),
(1, 1, 75.2, '2026-10-26T09:00:00Z'),
(1, 1, 79.2, '2026-10-26T10:00:00Z'),
(1, 1, 79.9, '2026-10-26T11:00:00Z'),
(1, 1, 77.3, '2026-10-26T12:00:00Z'),
(1, 1, 71.7, '2026-10-26T13:00:00Z'),
(1, 1, 63.7, '2026-10-26T14:00:00Z'),
(1, 1, 54.2, '2026-10-26T15:00:00Z'),
(1, 1, 44.3, '2026-10-26T16:00:00Z'),
(1, 1, 35.0, '2026-10-26T17:00:00Z'),
(1, 1, 27.3, '2026-10-26T18:00:00Z'),
(1, 1, 22.1, '2026-10-26T19:00:00Z'),
(1, 1, 20.0, '2026-10-26T20:00:00Z'),
(1, 1, 21.2, '2026-10-26T21:00:00Z'),
(2, 2, 80.0, '2026-10-26T06:00:00Z'),
(2, 2, 93.1, '2026-10-26T07:00:00Z'),
(2, 2, 104.7, '2026-10-26T08:00:00Z'),
(2, 2, 113.7, '2026-10-26T09:00:00Z'),
(2, 2, 118.9, '2026-10-26T10:00:00Z'),
(2, 2, 119.8, '2026-10-26T11:00:00Z'),
(2, 2, 116.4, '2026-10-26T12:00:00Z'),
(2, 2, 108.9, '2026-10-26T13:00:00Z'),
(2, 2, 98.3, '2026-10-26T14:00:00Z'),
(2, 2, 85.6, '2026-10-26T15:00:00Z'),
(2, 2, 72.4, '2026-10-26T16:00:00Z'),
(2, 2, 59.9, '2026-10-26T17:00:00Z'),
(2, 2, 49.7, '2026-10-26T18:00:00Z'),
(2, 2, 42.8, '2026-10-26T19:00:00Z'),
(2, 2, 40.0, '2026-10-26T20:00:00Z'),
(2, 2, 41.6, '2026-10-26T21:00:00Z'),
(3, 3, 60.0, '2026-10-26T06:00:00Z'),
(3, 3, 69.8, '2026-10-26T07:00:00Z'),
(3, 3, 78.6, '2026-10-26T08:00:00Z'),
(3, 3, 85.2, '2026-10-26T09:00:00Z'),
(3, 3, 89.2, '2026-10-26T10:00:00Z'),
(3, 3, 89.9, '2026-10-26T11:00:00Z'),
(3, 3, 87.3, '2026-10-26T12:00:00Z'),
(3, 3, 81.7, '2026-10-26T13:00:00Z'),
(3, 3, 73.7, '2026-10-26T14:00:00Z'),
(3, 3, 64.2, '2026-10-26T15:00:00Z'),
(3, 3, 54.3, '2026-10-26T16:00:00Z'),
(3, 3, 45.0, '2026-10-26T17:00:00Z'),
(3, 3, 37.3, '2026-10-26T18:00:00Z'),
(3, 3, 32.1, '2026-10-26T19:00:00Z'),
(3, 3, 30.0, '2026-10-26T20:00:00Z'),
(3, 3, 31.2, '2026-10-26T21:00:00Z');

-- Indexes for query acceleration
CREATE INDEX IF NOT EXISTS idx_meas_series_ts ON measurements (series_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_meas_ts ON measurements (timestamp);

