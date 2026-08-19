# Crew Experience

## Goal

A signed-in crew user should see information and controls for their current nganya assignment, while still being able to browse other public nganyas.

The crew area has three tabs:

- **Dashboard** — personal and assignment summary with operational controls.
- **My Nganya** — complete details and current location for the assigned nganya.
- **Nganyas** — read-only discovery view of other active nganyas.

## Fit with the current model

The user and assignment foundation required by this feature is already present:

- Crew users have account role `CREW`.
- Their job on a vehicle is stored separately as assignment role `DRIVER` or `CONDUCTOR`.
- A crew user can have only one active assignment.
- Crew vehicle endpoints derive the user from the authenticated session and reject operations on other vehicles.
- `GET /api/crew/vehicles` currently returns the signed-in user's assigned vehicle.
- Vehicle status, occupancy, location, route, gallery media, and public nganya APIs already exist.

The existing crew screen is a single page. It loads the assigned vehicle and can toggle online/offline, but it does not yet have the three-tab structure or a crew context response containing the user's assignment role.

## Navigation and routes

Use one shared crew shell with desktop and mobile navigation.

| Tab | Frontend route | Purpose |
|---|---|---|
| Dashboard | `/crew` | Personal summary and operational controls |
| My Nganya | `/crew/my-nganya` | Assigned vehicle profile and location |
| Nganyas | `/crew/nganyas` | Browse all public active nganyas |

All three routes require an authenticated account with role `CREW`.

The shell should show the product name, current tab, and sign-out action. It should use route links instead of local tab state so refresh, browser navigation, and deep linking work correctly.

## Crew context API

The dashboard needs both user and vehicle assignment data. The existing assigned-vehicle response alone cannot provide the signed-in user's assignment role.

Add:

`GET /api/crew/me`

Recommended response:

```json
{
  "user": {
    "id": "uuid",
    "name": "Jane Doe",
    "email": "jane@example.com"
  },
  "assignment": {
    "id": "uuid",
    "role": "DRIVER",
    "startedAt": "2026-08-19T10:00:00Z"
  },
  "vehicle": {
    "id": "uuid",
    "name": "Baba Yaga",
    "plateNumber": "Kxx 123X",
    "status": "ONLINE",
    "occupancyStatus": "LOW",
    "route": {
      "id": "uuid",
      "routeNumber": "111",
      "name": "Ngong Road Express",
      "origin": "CBD",
      "destination": "Ngong"
    },
    "latestLocation": null
  }
}
```

If the crew user has no active assignment, return `200 OK` with `assignment: null` and `vehicle: null`. This is an expected account state, not an authentication error. The UI should show an unassigned-state message and direct the user to contact an admin.

The current `GET /api/crew/vehicles` endpoint can remain temporarily for compatibility, but the crew pages should use `/api/crew/me` as their primary bootstrap request.

## Vehicle status semantics

Use the existing status values consistently:

- `ONLINE` — active and visible for live tracking.
- `OFFLINE` — not currently operating or trackable.
- `MAINTENANCE` — unavailable because it is being repaired or serviced.

Therefore:

- **Go Online** sets status to `ONLINE`.
- **Go Offline** sets status to `OFFLINE`.
- **Maintenance** sets status to `MAINTENANCE`
- Do not make **Go Offline** set `MAINTENANCE`; that would incorrectly report an ordinary end of service as a mechanical/service condition.

If crew should be allowed to declare maintenance, add a separate **Mark as Maintenance** action with its own confirmation. For the initial implementation, maintenance remains an admin-controlled status.

## Dashboard

### Summary

Show:

- Signed-in user's name.
- Assignment role (`DRIVER` or `CONDUCTOR`).
- Assigned nganya name and plate number.
- Assigned route number, name, origin, and destination.
- Current vehicle status.
- Current occupancy status.
- Latest location timestamp, when available.

### Quick actions

Provide two mutually exclusive primary actions:

- **Go Online** when the vehicle is not online.
- **Go Offline** when the vehicle is online.

Both actions must:

- Require confirmation.
- Disable while saving to prevent duplicate requests.
- Show the backend error without discarding the existing vehicle state.
- Refresh the crew context after success.

Use the existing endpoints:

- `POST /api/crew/vehicles/{vehicleId}/go-live`
- `POST /api/crew/vehicles/{vehicleId}/go-offline`

Do not show placeholder actions such as Update Route, Pit Stop, or Crew Chat unless they have an agreed workflow and API. Route assignment remains admin-controlled.

### Occupancy

Crew should be able to update occupancy independently of vehicle operational status:

- `LOW`
- `MEDIUM`
- `FULL`

The current `PATCH /api/crew/vehicles/{vehicleId}/status` requires both status and occupancy. Before execution, split this responsibility or add a focused endpoint:

`PATCH /api/crew/vehicles/{vehicleId}/occupancy`

```json
{
  "occupancyStatus": "MEDIUM"
}
```

This prevents a stale UI from accidentally overwriting the current vehicle status while changing occupancy.

## My Nganya

Show the signed-in user's assigned nganya only. Do not accept an arbitrary vehicle ID from the route.

### Vehicle details

Show:

- Name and plate number.
- Current status and occupancy.
- Verification and listing state where useful.
- Route number, name, origin, destination, and description.
- Wi-Fi availability.
- Sound system.
- Bass level.
- Screen count.
- Custom features.
- Gallery images.
- Other currently assigned crew members and their assignment roles.

The page is read-only except for explicitly permitted operational controls such as status, occupancy, and location. Crew cannot edit profile metadata, route assignment, gallery images, listing state, or crew assignments.

### Location map

The current crew screen displays coordinates over a decorative grid; it is not yet a functional map.

The initial implementation should:

- Show the latest saved latitude and longitude.
- Show the time of the latest update.
- Render a real marker when a map provider/component is available.
- Show a clear **Location not available** state when no update exists.
- Avoid presenting stale coordinates as live; label an update stale after an agreed threshold, recommended at five minutes while the vehicle is online.

For actual map tiles, reuse the same map component/provider selected for the commuter live map instead of introducing a crew-only mapping stack.

### Location sharing

The existing endpoint can save location:

`POST /api/crew/vehicles/{vehicleId}/location`

```json
{
  "latitude": -1.2921,
  "longitude": 36.8219,
  "speedKph": 24
}
```

Location sharing requires an explicit user action and browser permission. Recommended behavior:

1. Crew presses **Start sharing location**.
2. Explain why location is needed before requesting browser permission.
3. Use `navigator.geolocation.watchPosition` only after consent.
4. Throttle network updates; recommended no more than once every 15–30 seconds or after meaningful movement.
5. Stop the watcher when the user presses **Stop sharing**, signs out, the component is destroyed, or the vehicle goes offline.
6. Show permission-denied, unavailable, and timeout states.
7. Never claim sharing is active until the browser watcher starts successfully.

Going online should not silently start browser geolocation. The two actions can be presented together, but location permission must remain explicit.

## Nganyas

This tab is a read-only discovery experience for crew users.

- Reuse the public nganya list/search UI and public APIs.
- Show only vehicles with active public listings according to existing backend rules.
- Allow opening the public nganya profile.
- Do not expose admin edit actions.
- Do not expose another vehicle's crew-only controls.

The existing commuter routes currently allow only `TRAVELLER` in the frontend guard. The crew Nganyas tab should be placed under `/crew/nganyas` and reuse the public components there, rather than granting crew access to traveller routes globally.

## Authorization rules

- Every `/api/crew/**` endpoint requires account role `CREW`.
- Operational endpoints must derive the crew user from the authenticated session.
- The requested vehicle must match the user's active assignment.
- A suspended, invited, or unassigned user cannot perform vehicle operations.
- Public nganya endpoints remain read-only and can be used by the crew discovery tab.
- Assignment changes made by an admin should take effect on the next request; do not trust a vehicle ID cached only in the browser.

## Error and empty states

Define separate user-facing states:

- Not authenticated — redirect to login.
- Account is not active — deny login or show an account-status message.
- No active assignment — show the unassigned crew state.
- Assigned vehicle was hidden/suspended — crew context remains available, but public visibility is explained separately.
- Backend unavailable — keep cached screen state where possible and offer retry.
- Location unavailable or stale — show an explicit map fallback.
- Operation rejected because assignment changed — refresh context and explain that the user is no longer assigned to that nganya.

Do not collapse these into a generic “Could not reach the backend API” message.

## Backend changes required before UI execution

1. Add `GET /api/crew/me` returning user, active assignment, and vehicle context.
2. Add a focused occupancy update endpoint.
3. Decide whether maintenance remains admin-only; the recommended initial decision is yes.
4. Ensure active-account status is checked on authenticated requests, not only at login.
5. Add tests proving a crew user cannot operate another vehicle.
6. Add tests for an unassigned crew user and for assignment changes during a session.
7. Decide and document the stale-location threshold and location retention policy.

## Frontend changes required

1. Create a shared crew shell and three guarded child routes.
2. Create a typed crew-context API model and client method.
3. Refactor the current crew dashboard to use the context response.
4. Remove non-functional quick-action placeholders.
5. Build the read-only My Nganya details and gallery view.
6. Add occupancy controls.
7. Add explicit location-sharing controls and lifecycle cleanup.
8. Reuse the public nganya discovery components inside the crew shell.
9. Add specific loading, empty, stale-location, authorization, and API error states.

## Acceptance criteria

### Navigation

- A crew user can navigate directly to all three crew routes and refresh without losing the selected tab.
- A traveller or admin cannot open crew routes.
- Mobile and desktop navigation expose the same three destinations.

### Dashboard

- The dashboard displays the authenticated user's name and assignment role.
- It displays only the user's currently assigned nganya and route.
- Going online results in status `ONLINE`.
- Going offline results in status `OFFLINE`, not `MAINTENANCE`.
- Repeated clicks cannot send duplicate status requests.

### My Nganya

- The page displays profile, route, feature, crew, gallery, and latest-location data.
- It does not provide admin profile-editing controls.
- Missing or stale location is clearly identified.
- Location sharing starts only after explicit consent and stops reliably.

### Nganyas

- Crew can search and browse publicly visible nganyas.
- Opening another nganya never exposes crew operational controls.

### Security

- Changing a vehicle ID in an API request cannot operate another nganya.
- Moving or ending the user's assignment revokes access to the previous nganya on the next request.
- An unassigned crew user sees an empty assignment state and cannot call operational endpoints.

## Recommended execution order

1. Crew context and occupancy backend APIs with authorization tests.
2. Shared crew shell and guarded child routes.
3. Dashboard summary and online/offline controls.
4. My Nganya details, gallery, and location states.
5. Explicit browser location sharing.
6. Public Nganyas reuse under the crew shell.
7. Responsive, accessibility, and end-to-end verification.
