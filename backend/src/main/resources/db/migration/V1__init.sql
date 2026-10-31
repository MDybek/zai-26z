CREATE TABLE admin_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE access_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES admin_users (id) ON DELETE CASCADE
);

CREATE TABLE series (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    min_value REAL NOT NULL,
    max_value REAL NOT NULL,
    color TEXT NOT NULL,
    icon TEXT,
    unit TEXT
);

CREATE TABLE sensors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    series_id INTEGER NOT NULL,
    api_key_hash TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL,
    FOREIGN KEY (series_id) REFERENCES series (id) ON DELETE CASCADE
);

CREATE TABLE measurements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    series_id INTEGER NOT NULL,
    sensor_id INTEGER,
    value REAL NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (series_id) REFERENCES series (id) ON DELETE CASCADE,
    FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE SET NULL
);

CREATE INDEX idx_measurements_series_timestamp ON measurements (series_id, timestamp);
CREATE INDEX idx_measurements_timestamp ON measurements (timestamp);
CREATE INDEX idx_sensors_series ON sensors (series_id);
CREATE INDEX idx_access_tokens_hash ON access_tokens (token_hash);

CREATE TABLE error_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL,
    message TEXT NOT NULL
);
