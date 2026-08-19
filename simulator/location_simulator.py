#!/usr/bin/env python3
"""Local HTTP service that simulates an assigned nganya moving along its route."""

from __future__ import annotations

import json
import math
import os
import signal
import threading
import time
from dataclasses import asdict, dataclass
from http import HTTPStatus
from http.cookiejar import CookieJar
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import HTTPCookieProcessor, Request, build_opener

EARTH_RADIUS_METRES = 6_371_000


class SimulatorError(RuntimeError):
    pass


class KultureClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()))

    def request(self, method: str, path: str, body: dict[str, Any] | None = None) -> Any:
        data = json.dumps(body).encode() if body is not None else None
        request = Request(
            f"{self.base_url}{path}",
            data=data,
            method=method,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        try:
            with self.opener.open(request, timeout=10) as response:
                payload = response.read()
                return json.loads(payload) if payload else None
        except HTTPError as error:
            detail = error.read().decode(errors="replace")
            raise SimulatorError(f"API returned {error.code} for {path}: {detail}") from error
        except URLError as error:
            raise SimulatorError(f"Could not reach {self.base_url}: {error.reason}") from error

    def login(self, email: str, password: str) -> None:
        user = self.request("POST", "/api/auth/login", {"email": email, "password": password})
        if user.get("role") != "crew":
            raise SimulatorError(f"{email} is not a crew account")


def distance_metres(start: tuple[float, float], end: tuple[float, float]) -> float:
    lon1, lat1 = map(math.radians, start)
    lon2, lat2 = map(math.radians, end)
    delta_lat = lat2 - lat1
    delta_lon = lon2 - lon1
    value = math.sin(delta_lat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(delta_lon / 2) ** 2
    return 2 * EARTH_RADIUS_METRES * math.asin(math.sqrt(value))


def interpolate_route(coordinates: list[list[float]], spacing_metres: float) -> list[tuple[float, float]]:
    if len(coordinates) < 2:
        raise SimulatorError("The assigned route needs at least two geometry coordinates")
    points = [(float(point[0]), float(point[1])) for point in coordinates]
    result = [points[0]]
    carry = spacing_metres
    for start, end in zip(points, points[1:]):
        segment_length = distance_metres(start, end)
        if segment_length == 0:
            continue
        travelled = carry
        while travelled < segment_length:
            fraction = travelled / segment_length
            result.append((start[0] + (end[0] - start[0]) * fraction, start[1] + (end[1] - start[1]) * fraction))
            travelled += spacing_metres
        carry = travelled - segment_length
    if result[-1] != points[-1]:
        result.append(points[-1])
    return result


@dataclass
class SimulatorStatus:
    running: bool = False
    vehicleId: str | None = None
    vehicleName: str | None = None
    routeNumber: str | None = None
    pointIndex: int = 0
    pointCount: int = 0
    updatesSent: int = 0
    longitude: float | None = None
    latitude: float | None = None
    speedKph: float | None = None
    error: str | None = None


class LocationSimulator:
    def __init__(self, api_url: str, email: str, password: str) -> None:
        self.client = KultureClient(api_url)
        self.email = email
        self.password = password
        self.status = SimulatorStatus()
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.thread: threading.Thread | None = None

    def start(self, speed_kph: float, interval_seconds: float, loop: bool) -> dict[str, Any]:
        if speed_kph <= 0 or speed_kph > 160:
            raise SimulatorError("speedKph must be between 0 and 160")
        if interval_seconds < 0.25:
            raise SimulatorError("intervalSeconds must be at least 0.25")
        with self.lock:
            if self.status.running:
                raise SimulatorError("The simulator is already running")

        self.client.login(self.email, self.password)
        vehicle = self.client.request("GET", "/api/crew/vehicles")
        geometry = vehicle.get("route", {}).get("geometry")
        if not geometry or geometry.get("type") != "LineString":
            raise SimulatorError("The assigned vehicle's route has no GeoJSON LineString geometry; draw and save the route first")

        spacing = speed_kph * 1000 / 3600 * interval_seconds
        points = interpolate_route(geometry.get("coordinates", []), spacing)
        vehicle_id = vehicle["id"]
        self.client.request("POST", f"/api/crew/vehicles/{vehicle_id}/go-live", {})
        self.stop_event.clear()
        with self.lock:
            self.status = SimulatorStatus(
                running=True,
                vehicleId=vehicle_id,
                vehicleName=vehicle.get("name"),
                routeNumber=vehicle.get("route", {}).get("routeNumber"),
                pointCount=len(points),
                speedKph=speed_kph,
            )
        self.thread = threading.Thread(
            target=self._run,
            args=(vehicle_id, points, round(speed_kph), interval_seconds, loop),
            name="location-simulator",
            daemon=True,
        )
        self.thread.start()
        return self.snapshot()

    def _run(self, vehicle_id: str, points: list[tuple[float, float]], speed_kph: int, interval: float, loop: bool) -> None:
        try:
            while not self.stop_event.is_set():
                for index, (longitude, latitude) in enumerate(points):
                    if self.stop_event.is_set():
                        break
                    self.client.request(
                        "POST",
                        f"/api/crew/vehicles/{vehicle_id}/location",
                        {"latitude": latitude, "longitude": longitude, "speedKph": speed_kph},
                    )
                    with self.lock:
                        self.status.pointIndex = index
                        self.status.updatesSent += 1
                        self.status.longitude = longitude
                        self.status.latitude = latitude
                    self.stop_event.wait(interval)
                if not loop:
                    break
        except Exception as error:  # Keep the service alive and expose failures through /status.
            with self.lock:
                self.status.error = str(error)
        finally:
            with self.lock:
                self.status.running = False

    def stop(self, go_offline: bool = False) -> dict[str, Any]:
        self.stop_event.set()
        thread = self.thread
        if thread and thread is not threading.current_thread():
            thread.join(timeout=5)
        vehicle_id = self.status.vehicleId
        if go_offline and vehicle_id:
            self.client.request("POST", f"/api/crew/vehicles/{vehicle_id}/go-offline", {})
        return self.snapshot()

    def snapshot(self) -> dict[str, Any]:
        with self.lock:
            return asdict(self.status)


class RequestHandler(BaseHTTPRequestHandler):
    simulator: LocationSimulator

    def do_GET(self) -> None:
        if self.path in ("/health", "/status"):
            self.respond(HTTPStatus.OK, self.simulator.snapshot())
        else:
            self.respond(HTTPStatus.NOT_FOUND, {"error": "Not found"})

    def do_POST(self) -> None:
        try:
            body = self.read_json()
            if self.path == "/start":
                result = self.simulator.start(
                    float(body.get("speedKph", 35)),
                    float(body.get("intervalSeconds", 2)),
                    bool(body.get("loop", True)),
                )
            elif self.path == "/stop":
                result = self.simulator.stop(bool(body.get("goOffline", False)))
            else:
                self.respond(HTTPStatus.NOT_FOUND, {"error": "Not found"})
                return
            self.respond(HTTPStatus.OK, result)
        except (SimulatorError, ValueError, json.JSONDecodeError) as error:
            self.respond(HTTPStatus.BAD_REQUEST, {"error": str(error)})

    def read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        return json.loads(self.rfile.read(length)) if length else {}

    def respond(self, status: HTTPStatus, body: dict[str, Any]) -> None:
        payload = json.dumps(body, indent=2).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, message: str, *args: Any) -> None:
        print(f"[{self.log_date_time_string()}] {message % args}")


def main() -> None:
    api_url = os.getenv("KULTURE_API_URL", "http://localhost:8080")
    email = os.getenv("KULTURE_CREW_EMAIL", "")
    password = os.getenv("KULTURE_CREW_PASSWORD", "")
    host = os.getenv("SIMULATOR_HOST", "127.0.0.1")
    port = int(os.getenv("SIMULATOR_PORT", "8090"))
    if not email or not password:
        raise SystemExit("Set KULTURE_CREW_EMAIL and KULTURE_CREW_PASSWORD before starting the simulator")

    RequestHandler.simulator = LocationSimulator(api_url, email, password)
    server = ThreadingHTTPServer((host, port), RequestHandler)
    signal.signal(signal.SIGTERM, lambda *_: threading.Thread(target=server.shutdown).start())
    print(f"Location simulator listening on http://{host}:{port} (API: {api_url})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        RequestHandler.simulator.stop()
        server.server_close()


if __name__ == "__main__":
    main()
