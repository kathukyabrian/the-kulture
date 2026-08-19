# Nganya location simulator

This dependency-free Python service simulates the crew member's assigned nganya moving along its saved route geometry. It authenticates against the Spring Boot API, marks the nganya online, and posts interpolated location updates using the existing crew endpoints.

## Start the service

The API must be running, the crew account must be assigned to a vehicle, and that vehicle's route must have at least two points drawn and saved.

```bash
cd simulator
export KULTURE_CREW_EMAIL='crew@example.com'
export KULTURE_CREW_PASSWORD='your-password'
python3 location_simulator.py
```

Optional environment variables:

- `KULTURE_API_URL` defaults to `http://localhost:8080`.
- `SIMULATOR_HOST` defaults to `127.0.0.1`.
- `SIMULATOR_PORT` defaults to `8090`.

## Control the simulation

Start movement at 35 km/h, sending a coordinate every two seconds and looping at the end:

```bash
curl -X POST http://localhost:8090/start \
  -H 'Content-Type: application/json' \
  -d '{"speedKph":89,"intervalSeconds":2,"loop":true}'
```

Inspect the current simulated position:

```bash
curl http://localhost:8090/status
```

Stop movement while leaving the nganya online:

```bash
curl -X POST http://localhost:8090/stop
```

Stop movement and mark the nganya offline:

```bash
curl -X POST http://localhost:8090/stop \
  -H 'Content-Type: application/json' \
  -d '{"goOffline":true}'
```

The service uses GeoJSON coordinate order: `[longitude, latitude]`. Its API posts the corresponding `latitude` and `longitude` fields expected by Spring Boot.

## Tests

```bash
cd simulator
python3 -m unittest -v
```
