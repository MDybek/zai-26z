#!/usr/bin/env python3
"""IoT sensor emulator for the ZAI 26Z air-quality API.

Sends measurements only through POST /api/measurements with X-API-Key.
The API key is never hardcoded; pass --api-key or env SENSOR_API_KEY.
Supports both mathematical simulation (random/sine) and fetching real-world
air-quality measurements from the public Open-Meteo API.
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
    parser.add_argument("--api", required=True, help="API base URL, e.g. http://localhost:8080")
    parser.add_argument("--api-key", default=os.environ.get("SENSOR_API_KEY"), help="Sensor API key (or SENSOR_API_KEY)")
    parser.add_argument("--count", type=int, default=1, help="How many measurements to send (<=0 for infinite in live mode)")
    parser.add_argument("--interval", type=float, default=60.0, help="Seconds between live samples, or seconds between backfilled timestamps")
    parser.add_argument("--mode", choices=["random", "sine"], default="random", help="Simulation mode")
    parser.add_argument("--source", choices=["simulated", "open-meteo"], default="simulated",
                        help="Data source: simulated or live data from public Open-Meteo Air Quality API")
    parser.add_argument("--pollutant", choices=["pm2_5", "pm10", "nitrogen_dioxide"], default="pm2_5",
                        help="Pollutant to fetch from Open-Meteo API (used when --source open-meteo)")
    parser.add_argument("--latitude", type=float, default=52.23, help="Latitude for Open-Meteo (default: Warsaw 52.23)")
    parser.add_argument("--longitude", type=float, default=21.01, help="Longitude for Open-Meteo (default: Warsaw 21.01)")
    parser.add_argument("--min", dest="min_value", type=float, default=0.0, help="Minimum generated value")
    parser.add_argument("--max", dest="max_value", type=float, default=100.0, help="Maximum generated value")
    parser.add_argument("--from", dest="start", help="ISO-8601 start time for historical backfill, e.g. 2026-10-01T00:00:00Z")
    parser.add_argument("--live", action="store_true", help="Send readings continuously (or --count times) at --interval seconds")
    return parser.parse_args()


def fetch_open_meteo_value(lat: float, lon: float, pollutant: str) -> float:
    url = f"https://air-quality-api.open-meteo.com/v1/air-quality?latitude={lat}&longitude={lon}&current={pollutant}"
    req = urllib.request.Request(url, headers={"User-Agent": "ZAI-26Z-Sensor-Emulator/1.0"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        val = data.get("current", {}).get(pollutant)
        if val is None:
            raise ValueError(f"Pollutant '{pollutant}' not found in Open-Meteo response")
        return round(float(val), 2)


def value_at(args: argparse.Namespace, index: int) -> float:
    if args.source == "open-meteo":
        try:
            return fetch_open_meteo_value(args.latitude, args.longitude, args.pollutant)
        except Exception as err:
            print(f"[WARN] Failed to fetch from Open-Meteo: {err}. Falling back to simulation.", file=sys.stderr)
    if args.mode == "sine":
        mid = (args.min_value + args.max_value) / 2
        amplitude = (args.max_value - args.min_value) / 2
        return round(mid + amplitude * math.sin(index / 3.0), 2)
    return round(random.uniform(args.min_value, args.max_value), 2)


def post_measurement(api: str, api_key: str, value: float, timestamp: datetime | None) -> dict | None:
    body = {"value": value}
    if timestamp is not None:
        body["timestamp"] = timestamp.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    request = urllib.request.Request(
        api.rstrip("/") + "/api/measurements",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-API-Key": api_key,
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            payload = json.loads(response.read().decode("utf-8"))
            print(f"OK HTTP {response.status} id={payload.get('id')} value={payload.get('value')} timestamp={payload.get('timestamp')}")
            return payload
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        print(f"REJECTED HTTP {error.code}: {detail}", file=sys.stderr)
        if error.code in (400, 401, 422):
            print("The API rejected this measurement. Check sensor API key, timestamp and series [min, max] range.", file=sys.stderr)
        raise SystemExit(1)


def main() -> None:
    args = parse_args()
    if not args.api_key:
        print("Error: Provide --api-key or set SENSOR_API_KEY environment variable", file=sys.stderr)
        raise SystemExit(2)

    if args.live:
        sent = 0
        while args.count <= 0 or sent < args.count:
            post_measurement(args.api, args.api_key, value_at(args, sent), None)
            sent += 1
            if args.count <= 0 or sent < args.count:
                time.sleep(args.interval)
        return

    if args.start:
        start = datetime.fromisoformat(args.start.replace("Z", "+00:00"))
        for i in range(args.count):
            ts = start + timedelta(seconds=args.interval * i)
            post_measurement(args.api, args.api_key, value_at(args, i), ts)
        return

    for i in range(args.count):
        post_measurement(args.api, args.api_key, value_at(args, i), None)


if __name__ == "__main__":
    main()
