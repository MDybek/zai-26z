# ZAI 26Z – IoT Air Quality Measurement Platform

System do zbierania i wizualizacji serii pomiarowych z sensorów IoT (PM2.5, PM10, NO2).  
Projekt realizuje wymagania specyfikacji ZAI 26Z (F1–F13, T1–T11), kontrakt OpenAPI 3.1 (`docs/zai-api-26z.yaml`) oraz standardy WCAG 2.2 AA.

---

## Struktura Repozytorium

```text
zai-26z/
├── DOCUMENTATION.md      # Szablon dokumentacji końcowej (wymagane punkty 1-7 z PDF)
├── test.sh               # Skrypt uruchamiający testy backendu, frontendu i kontraktowe
├── sensor.py             # Emulator czujnika i generator danych (F11, F13 + Open-Meteo)
├── zai-tests.mjs         # Oficjalny zestaw testów automatycznych prowadzącego (E1/E2)
├── docker-compose.yml    # Orkiestracja kontenerów (Backend + Frontend + PostgreSQL + MongoDB + Generator)
├── backend/              # REST API w Spring Boot 4.1.1 (Java 25, PostgreSQL, MongoDB, Flyway, Lombok)
├── frontend/             # SPA w React 18 + TypeScript + Vite (wykres SVG, WCAG AA, druk)
├── docs/                 # Kontrakt OpenAPI (zai-api-26z.yaml)
└── task/                 # Oficjalna specyfikacja zadania (ZAI projekt 26Z.pdf)
```

---

## Szybki Start z Docker Compose (Rekomendowane)

Uruchomienie całego stosu (backend + frontend) jednym poleceniem:

```bash
docker compose up --build
```

- **Frontend (Aplikacja):** `http://localhost:3000`
- **Backend (API):** `http://localhost:8080` (Healthcheck: `http://localhost:8080/api/health`)
- **Baza PostgreSQL (Relacje):** port `5432` (`airquality`)
- **Baza MongoDB (Serie czasowe):** port `27017` (`airquality`)
- **Domyślny administrator:** `admin` / `Admin123!`

Zatrzymanie:
```bash
docker compose down
```

---

## Uruchomienie Lokalne (Bez Dockera)

### Wymagania
- Java 25 (lub 17+ z profilem jdk-legacy) (Eclipse Temurin / OpenJDK)
- Node.js 20+ i npm 10+
- Python 3.9+

### 1. Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```
Aplikacja uruchomi się pod adresem `http://localhost:3000` (Vite automatycznie przekierowuje `/api/*` na port 8080).

---

## Testowanie (`test.sh` & `zai-tests.mjs`)

Do uruchamiania testów służy skrypt w katalogu głównym:

```bash
# 1. Testy backendu (Spring Boot / JUnit / MockMvc):
./test.sh --backend

# 2. Weryfikacja frontendu (TypeScript & Vite build):
./test.sh --frontend

# 3. Oficjalne testy kontraktowe prowadzącego (przy uruchomionej aplikacji):
./test.sh --contract
# lub bezpośrednio:
node zai-tests.mjs --app http://localhost:3000 --api http://localhost:8080 --user admin --pass 'Admin123!' --stage E2
```

---

## Generator danych z czujników (`sensor.py`)

Skrypt przesyła pomiary wyłącznie przez API (`POST /api/measurements` z nagłówkiem `X-API-Key`).

```bash
# Automatyczne zainicjalizowanie 3 serii (PM2.5, PM10, NO2) i 16 pomiarów (F11):
python3 sensor.py

# Wysłanie danych do konkretnego czujnika:
python3 sensor.py --api http://localhost:8080 --api-key "<KLUCZ_API>" --count 16 --mode sine

# Pobieranie rzeczywistych danych jakości powietrza z Open-Meteo (+2 pkt bonus):
python3 sensor.py --api http://localhost:8080 --api-key "<KLUCZ_API>" --source open-meteo --pollutant pm2_5
```

---

## Dokumentacja

Szczegółowy opis architektury, diagram ERD oraz elementy bezpieczeństwa T5 znajdują się w pliku [DOCUMENTATION.md](DOCUMENTATION.md).
