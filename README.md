# ZAI 26Z – Air Quality Measurement API

Spring Boot 3 REST API for collecting and serving air-quality measurements from IoT sensors
(PM2.5, PM10, NO2). Implements the official course contract in `docs/zai-api-26z.yaml` (RFC 9457 Problem Details, Richardson Maturity Level 2).

---

## Quick Start with Docker (Recommended)

Start the entire application locally with Docker Compose:

```bash
docker compose up --build
```

The API will be available at: `http://localhost:8080` (Healthcheck: `http://localhost:8080/api/health`).

Data is persisted on the host in `./data/airquality.db`.

To stop the application:
```bash
docker compose down
```

---

## Local Development (Without Docker)

Requirements:
- Java 17+ (e.g. OpenJDK 17)
- Node.js 20+ (for official automated test suite `docs/zai-tests.mjs`)
- Python 3.9+ (for sensor emulator `generator/sensor.py`)

### 1. Build and Run

```bash
# Build package and run tests
./mvnw clean test

# Run application
./mvnw spring-boot:run
```

Alternatively, run the packaged JAR:
```bash
./mvnw clean package -DskipTests
java -jar target/air-quality-api-1.0.0.jar
```

Default administrator credentials (configured via `.env` or environment variables):
- **Username**: `admin`
- **Password**: `Admin123!`

On first startup, the application:
1. Executes database migrations via Flyway (`db/migration/V1__init.sql`).
2. Bootstraps the default admin account with BCrypt password hashing.
3. Automatically seeds 3 default series (PM2.5, PM10, NO2) with 16 measurements each, fulfilling requirement F11.

---

## IoT Sensor Emulator (F13 & Bonus)

The emulator tool is located in `generator/sensor.py`. It sends measurements strictly through the API (`POST /api/measurements` with the `X-API-Key` header).

### 1. Register a Sensor in the Application
Authenticate as administrator and register a sensor:
```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' | jq -r .accessToken)

# 2. Register sensor for series 1
KEY=$(curl -s -X POST http://localhost:8080/api/sensors \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Living Room Sensor","seriesId":1}' | jq -r .apiKey)

echo "Sensor API Key: $KEY"
```

### 2. Backfill Historical Data
```bash
python3 generator/sensor.py \
  --api http://localhost:8080 \
  --api-key "$KEY" \
  --count 16 \
  --interval 3600 \
  --mode sine \
  --min 5 --max 40 \
  --from 2026-10-01T00:00:00Z
```

### 3. Continuous Live Streaming
```bash
python3 generator/sensor.py \
  --api http://localhost:8080 \
  --api-key "$KEY" \
  --live \
  --interval 5 \
  --mode random \
  --min 10 --max 60
```

### 4. Real-world Data Fetching (Bonus Feature: +2 pts)
Fetch live measurements directly from the public Open-Meteo Air Quality API:
```bash
python3 generator/sensor.py \
  --api http://localhost:8080 \
  --api-key "$KEY" \
  --source open-meteo \
  --pollutant pm2_5 \
  --latitude 52.23 \
  --longitude 21.01
```

---

## Testing & Verification

### Unit & Integration Tests (MockMvc & SpringBootTest)
```bash
./mvnw test
```

### Official Contract Automated Test Suite
To run the course testing suite against your running instance:
```bash
node docs/zai-tests.mjs --app http://localhost:8080 --user admin --pass 'Admin123!' --stage E1
node docs/zai-tests.mjs --app http://localhost:8080 --user admin --pass 'Admin123!' --stage E2
```

---

## Security Architecture (T5 Compliance)

1. **Password Hashing**: BCrypt with salted rounds (`BCryptPasswordEncoder`).
2. **SQL Injection Protection**: Parameterized queries via Spring Data JPA and Hibernate ORM.
3. **Session & Tokens**: Bearer tokens with strict expiration (1h default) stored as SHA-256 hashes in DB, accompanied by `HttpOnly; Secure; SameSite=Strict` cookies.
4. **Server-Side Authorization**: Enforced in Spring Security filter chain on all mutating endpoints (`/api/series`, `/api/sensors`, `/api/auth/password`).
5. **Sensor Authentication**: Cryptographically random 32-byte hexadecimal keys, stored only as SHA-256 hashes, displayed only once upon registration.
6. **Security Headers**:
   - `Content-Security-Policy`
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `Referrer-Policy: no-referrer`
7. **Error Disclosure Prevention**: Stack traces and internal exceptions suppressed via RFC 9457 Problem Details (`application/problem+json`).
