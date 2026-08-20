# Crew Coordinate Broadcasting

## Implementation status

Implemented in the repository:

- validated, idempotent batch ingestion at `POST /api/crew/vehicles/{vehicleId}/locations`;
- accuracy, heading, client timestamp, server receipt timestamp, session ID, and sample ID persistence;
- a one-row-per-vehicle latest-location projection;
- authenticated assignment enforcement and online-state enforcement;
- accepted, duplicate, and rejected sample acknowledgements;
- Spring MVC SSE at `GET /api/vehicles/location-events`, including heartbeat and reconnect IDs;
- Angular SSE consumption with 30-second REST fallback polling;
- a persistent foreground-web queue with movement/heartbeat filtering and exponential retry;
- an Android fused-location foreground service with an ongoing notification and stop action;
- stale-position labeling after 60 seconds.

Operational hardening still recommended before high-scale deployment:

- persist the native Android retry queue across process termination;
- add explicit server-side broadcast-session start/stop records;
- add per-account/device rate limiting and production metrics;
- add Redis or another broker before horizontally scaling the API;
- field-test battery use, process death, revoked permissions, and poor connectivity on physical devices.

## Purpose

This document defines the recommended approach for broadcasting a crew member's device coordinates as the live position of their assigned nganya. It covers collection on Android, delivery to the Spring Boot API, storage, live distribution to map viewers, reliability, privacy, and rollout.

## Recommendation

Use two separate channels:

1. **Crew device to API:** authenticated HTTPS `POST` requests, with a small persistent retry queue on the device.
2. **API to map viewers:** Server-Sent Events (SSE) for live updates, with the existing REST endpoint retained for initial state and fallback polling.

Do not use a permanent WebSocket connection to upload coordinates. Location samples are small, infrequent, independently retryable messages. HTTPS is simpler to secure, observe, retry, and operate through mobile networks and proxies. SSE is a good fit for viewers because updates flow only from server to client.

For reliable tracking while the app is minimized or the screen is locked, coordinate collection must run through an Android location foreground service with a persistent notification. Browser `navigator.geolocation.watchPosition` should remain the web and foreground-only fallback.

## Current implementation

The crew UI currently:

- asks for explicit consent before sharing;
- starts `navigator.geolocation.watchPosition` with high accuracy;
- accepts cached positions up to 10 seconds old;
- limits uploads to one every 15 seconds;
- derives speed from the browser position;
- stops sharing on user request, logout, going offline, or component destruction;
- sends `POST /api/crew/vehicles/{vehicleId}/location`;
- refreshes crew context after a successful upload.

The API currently:

- authenticates the crew session;
- confirms that the crew member is assigned to the requested vehicle;
- inserts each accepted sample into `vehicle_locations`;
- marks the vehicle `ONLINE`;
- returns the full vehicle detail response.

This works while the crew screen remains open and active. It is not a dependable background tracking mechanism because Android limits background location access and may suspend the WebView.

## Proposed flow

```text
Android location foreground service
        |
        | samples position and applies filters
        v
Persistent device queue
        |
        | HTTPS batch upload with retry
        v
Spring Boot location ingest endpoint
        |                 |
        |                 +--> latest-location cache
        v
location history database
        |
        +--> SSE update stream --> public maps
```

### 1. Collect coordinates

Use Android's fused location provider inside a foreground service for installed Android builds. The service must be started by an explicit crew action while the app is visible and must display an ongoing notification such as “The Kulture is sharing The Matrix's location.”

Suggested sampling policy:

| Vehicle state | Sample target | Upload target |
|---|---:|---:|
| Moving | every 5–10 seconds | every 10–15 seconds |
| Stationary | every 30–60 seconds | every 60 seconds |
| Offline or sharing stopped | disabled | disabled |

Upload immediately when any of these occurs:

- movement since the last accepted sample is at least 25 metres;
- heading changes substantially at normal road speed;
- 30 seconds have passed while moving;
- the first valid fix is acquired;
- connectivity returns and queued samples exist.

Reject a device sample before upload when:

- latitude or longitude is outside its valid range;
- accuracy is missing or worse than an agreed threshold, initially 50 metres;
- its timestamp is older than the last accepted device sample;
- it implies an impossible jump, for example several kilometres in a few seconds;
- it is effectively identical to the previous sample and the heartbeat interval has not elapsed.

The web build can continue using `navigator.geolocation.watchPosition`, but the UI must label it as foreground-only sharing.

### 2. Queue and upload

Store unsent samples in a small persistent device queue. Do not rely only on memory because the operating system can terminate the app.

Use a batch endpoint:

`POST /api/crew/vehicles/{vehicleId}/locations`

```json
{
  "sessionId": "69bbd43e-53a0-4d70-96e7-86a941f536a9",
  "samples": [
    {
      "sampleId": "01K4...",
      "latitude": -1.2921,
      "longitude": 36.8219,
      "accuracyMeters": 8.4,
      "speedKph": 24.1,
      "headingDegrees": 132.0,
      "recordedAt": "2026-08-20T09:15:12Z"
    }
  ]
}
```

Recommended rules:

- Allow 1–20 samples per request.
- Limit the request body to a small fixed size.
- Treat `sampleId` as an idempotency key so retries cannot create duplicates.
- Return accepted, duplicate, and rejected sample IDs.
- Use exponential backoff with jitter for network or `5xx` errors: approximately 2, 5, 15, 30, then 60 seconds.
- Do not retry `400`, `401`, or `403` indefinitely. Stop sharing and prompt the user when authorization is lost.
- Keep at most 30–60 minutes of queued data and discard the oldest samples when the limit is reached.

Keep the existing single-sample endpoint during migration. It can call the same ingest service as the batch endpoint.

### 3. Validate on the server

Server validation is authoritative. For every batch:

1. Authenticate the session or mobile access token.
2. Resolve the active crew assignment from the authenticated principal.
3. Reject a `vehicleId` that is not the assigned vehicle.
4. Validate coordinate ranges, accuracy, speed, heading, and timestamps.
5. Reject samples too far in the future or beyond the accepted history window.
6. Deduplicate by `sampleId`.
7. Store the accepted client `recordedAt` and a separate server `receivedAt`.
8. Update the vehicle's latest-location projection only when the sample is newer.
9. Publish one latest-position event after the transaction commits.

The request should not automatically make a deliberately offline vehicle online. Sharing should require an active broadcast session created when the crew presses **Go online and share location**. This avoids a delayed retry unexpectedly changing vehicle state.

### 4. Store current position and history separately

Avoid querying the full history table to render every map refresh.

Maintain:

- `vehicle_latest_locations`: one row per vehicle, updated with the newest valid sample;
- `vehicle_locations`: append-only history used for route analysis, debugging, and audit;
- optional Redis latest-position entries when multiple API instances or high viewer volume justify it.

Recommended indexes:

```text
unique(sample_id)
(vehicle_id, recorded_at desc)
(recorded_at)
```

Apply a retention policy to detailed history. A reasonable starting point is 7–30 days, followed by aggregation or deletion. The exact duration should be approved as a product and privacy decision.

### 5. Broadcast to viewers

Continue loading initial map state through the existing vehicle REST endpoint. Then subscribe to an SSE endpoint such as:

`GET /api/vehicles/location-events`

Example event:

```text
event: vehicle-location
id: 01K4...
data: {"vehicleId":"...","latitude":-1.2921,"longitude":36.8219,"speedKph":24.1,"recordedAt":"2026-08-20T09:15:12Z"}
```

SSE guidance:

- send only public, active, verified vehicles;
- send a heartbeat comment every 15–30 seconds so proxies keep the stream open;
- support reconnect using the SSE event ID where practical;
- coalesce bursts so a viewer receives at most one event per vehicle every 2–5 seconds;
- retain the current 5-second polling path as a fallback during rollout;
- if running multiple API instances, distribute accepted updates through Redis Pub/Sub or a durable message broker before emitting SSE.

Spring MVC supports SSE through `SseEmitter`; adopting the full reactive WebFlux stack is not required solely for this feature.

## Authentication and security

- Use TLS for every upload and viewer stream.
- Never accept a crew/user ID from the request body as authorization.
- Derive the assigned vehicle from the authenticated principal and verify it on every upload.
- Rate-limit per user, device, and vehicle.
- Record assignment ID, account ID, device session ID, and server receipt time for auditing.
- Rotate or terminate the broadcast session on sign-out, assignment removal, suspension, or explicit stop.
- Do not expose crew identity, device identifiers, accuracy, or historical trails through the public live-map response.
- Detect concurrent broadcasters for one vehicle. Either select one active session or define a deterministic rule rather than alternating positions from two devices.

For an installed mobile app, a short-lived bearer access token plus refresh-token flow is more reliable than a third-party WebView session cookie. Cookie authentication can remain for the same-origin web application.

## Permissions and privacy

Location sharing must be obvious and user-controlled:

- explain the purpose before the system permission prompt;
- request foreground location first;
- request background access only if continuous tracking is an essential product feature;
- show a persistent Android notification while background sharing runs;
- provide a visible stop action in both the app and notification;
- stop when the vehicle goes offline, the assignment ends, or authorization is revoked;
- document retention and who can see current and historical data.

Android limits background location updates and applies additional permission and foreground-service requirements. Google Play also restricts background location to apps where it is core functionality. See [Android background location guidance](https://developer.android.com/develop/sensors-and-location/location/background), [background location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions/background), and [foreground service launch requirements](https://developer.android.com/develop/background-work/services/fgs/launch).

## Failure and UI states

The crew UI should distinguish:

- acquiring location;
- sharing normally;
- sharing with queued/offline samples;
- low accuracy;
- permission denied;
- device location disabled;
- session expired or assignment revoked;
- background sharing unavailable;
- stopped by the user.

Do not show “sharing is active” immediately after creating a watcher. Show it only after receiving a valid fix and confirming at least one upload, or clearly distinguish “acquiring location” from “broadcasting.”

The public map should label a vehicle stale after two missed heartbeat windows. With a 15-second moving upload target, mark it stale after approximately 60 seconds and hide or mark it offline after an agreed longer threshold such as five minutes.

## Observability

Track at least:

- accepted, rejected, duplicate, and rate-limited samples;
- upload latency from `recordedAt` to `receivedAt`;
- active broadcast sessions;
- queue depth and retry count reported by clients;
- last valid update age per online vehicle;
- SSE connections, reconnects, and delivery errors;
- battery-impact and data-use measurements during field testing.

Use structured logs without logging exact coordinates at normal information level.

## Rollout plan

### Phase 1: strengthen the existing foreground implementation

- Extend the payload with `sampleId`, `recordedAt`, `accuracyMeters`, and optional heading.
- Add server-side coordinate and timestamp validation.
- Stop returning the full vehicle detail payload for every location upload; return `202 Accepted` or a small acknowledgement.
- Add idempotency and a latest-location table/projection.
- Improve UI status and error reporting.

### Phase 2: reliable Android broadcasting

- Add a Capacitor plugin backed by Android fused location and a location foreground service.
- Add the persistent queue and batch endpoint.
- Introduce explicit broadcast sessions.
- Test screen-off, app-backgrounded, process-killed, poor-network, and permission-revoked scenarios on physical devices.

### Phase 3: live viewer delivery

- Add SSE from Spring MVC.
- Update the Angular map incrementally rather than polling the complete vehicle list.
- Add a broker only when multiple API replicas require cross-instance delivery.

## Acceptance criteria

- Crew can explicitly start and stop location sharing.
- An assigned vehicle updates on public maps within 20 seconds under normal connectivity.
- No other crew account can update that vehicle.
- Duplicate retries do not create duplicate history rows.
- Temporary loss of connectivity does not lose recent samples.
- Background sharing has a persistent Android notification and survives screen lock.
- Going offline, signing out, losing assignment, or stopping sharing terminates broadcasting.
- Public viewers can identify stale positions and never see them presented as current.
- Battery and mobile-data usage remain acceptable during a full operating shift.

## Decision summary

The best near-term architecture is **native Android foreground location collection + queued HTTPS batch ingestion + latest-position projection + SSE viewer updates**. It fits the current Angular/Capacitor and Spring MVC application, improves reliability incrementally, and avoids adding WebSocket complexity where bidirectional continuous messaging is not needed.

Spring references: [Spring MVC asynchronous requests and SSE](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html) and [Spring SSE API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/codec/ServerSentEvent.html).
