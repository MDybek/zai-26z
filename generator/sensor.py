#!/usr/bin/env python3
"""IoT sensor emulator and data seeder for the ZAI 26Z air-quality API.

Sends measurements strictly through POST /api/measurements with X-API-Key.
If no API key is provided, it can automatically authenticate with the API,
register a sensor, obtain its key, and send measurements.
Supports --seed to automatically initialize all 3 required series, sensors,
and 16 measurements each.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import random
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Emulate an air-quality sensor and post measurements to the API")
    parser.add_argument("--api", default="http://localhost:8080", help="API base URL (default: http://localhost:8080)")
    parser.add_argument("--api-key", default=os.environ.get("SENSOR_API_KEY"), help="Sensor API key (or SENSOR_API_KEY env var)")
    parser.add_argument("--user", default=os.environ.get("ADMIN_USERNAME", "admin"), help="Admin username for auto-registration (default: admin)")
    parser.add_argument("--password", default=os.environ.get("ADMIN_PASSWORD", "Admin123!"), help="Admin password for auto-registration (default: Admin123!)")
    parser.add_argument("--series", type=int, default=None, help="Target series ID (default: seeds all 3 series if omitted)")
    parser.add_argument("--seed", action="store_true", help="Auto-seed all 3 series (PM2.5, PM10, NO2) and 16 measurements each via API (F11)")
    parser.add_argument("--count", type=int, default=16, help="How many measurements to send per series (default: 16)")
    parser.add_argument("--interval", type=float, default=3600.0, help="Seconds between timestamps (default: 3600s = 1h)")
    parser.add_argument("--mode", choices=["random", "sine"], default="sine", help="Simulation mode (default: sine)")
    parser.add_argument("--source", choices=["simulated", "open-meteo"], default="simulated",
                        help="Data source: simulated or live data from public Open-Meteo Air Quality API")
    parser.add_argument("--pollutant", choices=["pm2_5", "pm10", "nitrogen_dioxide"], default="pm2_5",
                        help="Pollutant for Open-Meteo API (used when --source open-meteo)")
    parser.add_argument("--latitude", type=float, default=52.23, help="Latitude for Open-Meteo (default: Warsaw 52.23)")
    parser.add_argument("--longitude", type=float, default=21.01, help="Longitude for Open-Meteo (default: Warsaw 21.01)")
    parser.add_argument("--min", dest="min_value", type=float, default=10.0, help="Minimum generated value")
    parser.add_argument("--max", dest="max_value", type=float, default=50.0, help="Maximum generated value")
    parser.add_argument("--from", dest="start", help="ISO-8601 start time for backfill (default: past)")
    parser.add_argument("--live", action="store_true", help="Send readings continuously at --interval seconds")
    return parser.parse_args()


def clean_api_key(raw: str | None) -> str | None:
    if not raw:
        return None
    val = raw.strip()
    if val in ("", "null", "undefined", "None", "$SENSOR_API_KEY"):
        return None
    return val


def api_request(method: str, url: str, data: dict | None = None, token: str | None = None, api_key: str | None = None) -> tuple[int, dict | list | None]:
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "User-Agent": "ZAI-26Z-Sensor-Emulator/2.0",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if api_key:
        headers["X-API-Key"] = api_key

    payload = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=payload, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            content = resp.read().decode("utf-8")
            return resp.status, json.loads(content) if content else None
    except urllib.error.HTTPError as err:
        content = err.read().decode("utf-8", errors="replace")
        err_json = None
        try:
            err_json = json.loads(content)
        except Exception:
            pass
        return err.code, err_json if err_json else {"detail": content}


def login_admin(api: str, user: str, password: str) -> str:
    url = f"{api.rstrip('/')}/api/auth/login"
    status, body = api_request("POST", url, {"username": user, "password": password})
    if status == 200 and isinstance(body, dict) and "accessToken" in body:
        return body["accessToken"]
    raise RuntimeError(f"Admin login failed (HTTP {status}): {body}")


def get_series_list(api: str) -> list[dict]:
    url = f"{api.rstrip('/')}/api/series"
    status, body = api_request("GET", url)
    if status == 200 and isinstance(body, list):
        return body
    return []


def create_series(api: str, token: str, name: str, min_val: float, max_val: float, color: str, unit: str) -> int:
    # Check existing series
    existing = get_series_list(api)
    for s in existing:
        if s.get("name") == name:
            print(f"  [INFO] Series '{name}' already exists (id={s['id']})")
            return s["id"]

    url = f"{api.rstrip('/')}/api/series"
    payload = {
        "name": name,
        "minValue": min_val,
        "maxValue": max_val,
        "color": color,
        "unit": unit
    }
    status, body = api_request("POST", url, payload, token=token)
    if status == 201 and isinstance(body, dict) and "id" in body:
        print(f"  [OK] Created series '{name}' (id={body['id']}, range=[{min_val}, {max_val}], color={color})")
        return body["id"]
    raise RuntimeError(f"Failed to create series '{name}' (HTTP {status}): {body}")


def register_sensor(api: str, token: str, series_id: int, name: str) -> str:
    url = f"{api.rstrip('/')}/api/sensors"
    payload = {"name": name, "seriesId": series_id}
    status, body = api_request("POST", url, payload, token=token)
    if status == 201 and isinstance(body, dict) and "apiKey" in body:
        key = body["apiKey"]
        print(f"  [OK] Registered sensor '{name}' for series {series_id}")
        print(f"       -> Generated API Key: {key}")
        return key
    raise RuntimeError(f"Failed to register sensor (HTTP {status}): {body}")


def post_measurement(api: str, api_key: str, value: float, timestamp: datetime | None) -> dict | None:
    url = f"{api.rstrip('/')}/api/measurements"
    body = {"value": value}
    if timestamp is not None:
        body["timestamp"] = timestamp.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    status, resp = api_request("POST", url, body, api_key=api_key)
    if status == 201 and isinstance(resp, dict):
        print(f"  OK HTTP {status} id={resp.get('id')} value={resp.get('value')} timestamp={resp.get('timestamp')}")
        return resp
    else:
        print(f"  REJECTED HTTP {status}: {resp}", file=sys.stderr)
        if status in (400, 401, 422):
            print("  The API rejected this measurement. Check sensor API key, timestamp and series range.", file=sys.stderr)
        sys.exit(1)


def fetch_open_meteo_value(lat: float, lon: float, pollutant: str) -> float:
    url = f"https://air-quality-api.open-meteo.com/v1/air-quality?latitude={lat}&longitude={lon}&current={pollutant}"
    req = urllib.request.Request(url, headers={"User-Agent": "ZAI-26Z-Sensor-Emulator/2.0"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        val = data.get("current", {}).get(pollutant)
        if val is None:
            raise ValueError(f"Pollutant '{pollutant}' not found in Open-Meteo response")
        return round(float(val), 2)


def value_at(args: argparse.Namespace, index: int, min_val: float, max_val: float) -> float:
    if args.source == "open-meteo":
        try:
            return fetch_open_meteo_value(args.latitude, args.longitude, args.pollutant)
        except Exception as err:
            print(f"[WARN] Failed to fetch from Open-Meteo: {err}. Falling back to simulation.", file=sys.stderr)
    if args.mode == "sine":
        mid = (min_val + max_val) / 2
        amplitude = (max_val - min_val) / 2
        return round(mid + amplitude * math.sin(index / 3.0), 2)
    return round(random.uniform(min_val, max_val), 2)


def seed_all_series(api: str, user: str, password: str, count: int = 16, mode: str = "sine") -> None:
    print(f"\n=======================================================")
    print(f" ZAI 26Z Auto-Seeder: Filling database via public API")
    print(f" Target API: {api}")
    print(f" Series: PM2.5, PM10, NO2 | Measurements: {count} each")
    print(f" Mode: {mode}")
    print(f"=======================================================\n")

    print("[Step 1/4] Authenticating with administrator credentials...")
    token = login_admin(api, user, password)
    print("  [OK] Admin authentication successful.\n")

    series_configs = [
        {"name": "PM2.5", "min": 0.0, "max": 150.0, "color": "#1f77b4", "unit": "µg/m³", "gen_min": 20.0, "gen_max": 80.0},
        {"name": "PM10",  "min": 0.0, "max": 250.0, "color": "#ff7f0e", "unit": "µg/m³", "gen_min": 40.0, "gen_max": 120.0},
        {"name": "NO2",   "min": 0.0, "max": 200.0, "color": "#2ca02c", "unit": "µg/m³", "gen_min": 30.0, "gen_max": 90.0},
    ]

    base_time = datetime.now(timezone.utc) - timedelta(hours=count + 4)

    for i, cfg in enumerate(series_configs, 1):
        print(f"[Step {i+1}/4] Processing Series '{cfg['name']}'...")
        series_id = create_series(api, token, cfg["name"], cfg["min"], cfg["max"], cfg["color"], cfg["unit"])
        sensor_name = f"{cfg['name']} Sensor"
        api_key = register_sensor(api, token, series_id, sensor_name)

        print(f"  Sending {count} measurements for {cfg['name']} via API...")
        mid = (cfg["gen_min"] + cfg["gen_max"]) / 2
        amp = (cfg["gen_max"] - cfg["gen_min"]) / 2
        for step in range(count):
            ts = base_time + timedelta(hours=step)
            if mode == "sine":
                val = round(mid + amp * math.sin(step / 3.0), 1)
            else:
                val = round(random.uniform(cfg["gen_min"], cfg["gen_max"]), 1)
            post_measurement(api, api_key, val, ts)
        print(f"  [OK] {count} measurements successfully added for {cfg['name']}.\n")

    print("=======================================================")
    print(f" [SUCCESS] All 3 series populated with {count} points each!")
    print(" Fully compliant with F11 requirement (seeded via API).")
    print("=======================================================\n")


def main() -> None:
    args = parse_args()
    api_key = clean_api_key(args.api_key)

    # Mode 1: Explicit --seed or default run when no key and no specific series passed
    # This ensures `python3 generator/sensor.py` or running with empty key does all the work!
    if args.seed or (not api_key and args.series is None and not args.live):
        print("[INFO] No sensor key or series specified. Auto-seeding all 3 required series (PM2.5, PM10, NO2)...")
        try:
            seed_all_series(args.api, args.user, args.password, args.count, args.mode)
            return
        except Exception as e:
            print(f"[ERROR] Auto-seeding failed: {e}", file=sys.stderr)
            sys.exit(2)

    # Mode 2: If a specific series is specified without an API key, auto-register for that series
    if not api_key:
        print(f"[INFO] No --api-key provided. Auto-registering sensor for series {args.series}...")
        try:
            token = login_admin(args.api, args.user, args.password)
            series_list = get_series_list(args.api)
            target_series_id = args.series or 1

            # If specified series doesn't exist yet, create default PM2.5 series
            if not any(s.get("id") == target_series_id for s in series_list):
                target_series_id = create_series(args.api, token, "PM2.5", 0.0, 150.0, "#1f77b4", "µg/m³")

            api_key = register_sensor(args.api, token, target_series_id, f"Auto-Sensor-Series-{target_series_id}")
            print(f"[INFO] Using auto-registered API Key: {api_key}\n")
        except Exception as e:
            print(f"[ERROR] Failed to auto-register sensor: {e}", file=sys.stderr)
            print("Provide --api-key or verify admin credentials (--user, --password)", file=sys.stderr)
            sys.exit(2)

    # Mode 3: Continuous Live Streaming
    if args.live:
        print(f"[INFO] Starting live measurement transmission (interval: {args.interval}s)...")
        sent = 0
        while args.count <= 0 or sent < args.count:
            val = value_at(args, sent, args.min_value, args.max_value)
            post_measurement(args.api, api_key, val, None)
            sent += 1
            if args.count <= 0 or sent < args.count:
                time.sleep(args.interval)
        return

    # Mode 4: Historical Backfill
    if args.start:
        start = datetime.fromisoformat(args.start.replace("Z", "+00:00"))
    else:
        start = datetime.now(timezone.utc) - timedelta(seconds=args.interval * args.count)

    print(f"[INFO] Posting {args.count} measurements starting from {start.isoformat()}...")
    for i in range(args.count):
        ts = start + timedelta(seconds=args.interval * i)
        val = value_at(args, i, args.min_value, args.max_value)
        post_measurement(args.api, api_key, val, ts)

    print(f"\n[DONE] Sent {args.count} measurements successfully.")


if __name__ == "__main__":
    main()
