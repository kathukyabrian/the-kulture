# The Kulture Project Overview

## Purpose

The Kulture is a mobile-first live tracking app for Nairobi matatu culture. The first version should let commuters discover nearby nganyas, view route and vehicle details, and see simple live status updates. A lightweight crew/admin experience should support going live, viewing basic performance, and managing fleet verification.

The product should stay intentionally simple: clear screens, small APIs, straightforward database tables, and no complex workflow until the core tracking loop works.

## Design Direction

The visual source is `docs/stitch_nganya_live_tracker.zip`.

The design system is named **Nairobi Nights**. It uses a dark charcoal base with neon accents:

- Primary: electric purple for brand, active route paths, and primary actions.
- Secondary: lime green for available, on-time, and positive states.
- Tertiary: hot pink for live pings, alerts, and energetic accents.
- Surfaces: dark charcoal tiers with rim-light borders instead of heavy shadows.
- Typography: Anybody for display/headlines, Hanken Grotesk for body text, JetBrains Mono for route numbers, plates, timestamps, and telemetry.

Reference screens in the zip:

- `live_map_search`: commuter map, route search, nearby nganya bottom sheet, bottom navigation, contextual action button.
- `nganya_profile`: vehicle profile, route badge, live tracking state, feature stats, crew rating, gallery.
- `fleet_overview`: admin dashboard with active vehicles, alerts, revenue, live map, pending verifications, crew performance, fleet distribution.
- `crew_dashboard`: crew mobile dashboard with go-live control, route status, earnings, quick actions, and current location preview.

The Angular implementation should translate these static HTML references into reusable Angular components and Tailwind styles while preserving the mobile app feel.

## Technical Stack

### Backend

- Spring Boot API.
- PostgreSQL database.
- Liquibase for database migrations.
- Package architecture:
  - `domain`: JPA entities and enums.
  - `repository`: Spring Data repositories.
  - `service`: simple business logic and data shaping.
  - `web.rest`: REST controllers and request/response DTOs.

### Frontend

- Angular.
- Tailwind CSS.
- Mobile-first responsive layouts.
- App-ready navigation and touch targets.
- Componentized screens based on the Stitch references.

## Current Repository State

- `api/` contains a minimal Spring Boot application.
- `app/` contains a default Angular starter application.
- `docs/PROMPT.md` defines the desired architecture and design source.
- `docs/stitch_nganya_live_tracker.zip` contains the visual references.

## Core Users

- **Commuter:** searches routes, views nearby nganyas, checks live status, opens vehicle profiles.
- **Crew:** turns tracking on/off, updates route status, views commuter interest and daily numbers.
- **Fleet/Admin:** monitors vehicles, reviews pending verifications, checks alerts and basic performance.

## MVP Functional Scope

### Commuter Experience

- View live map/search screen.
- Search by route number, route name, destination, or vehicle name.
- See nearby active nganyas with route, ETA, occupancy, and status.
- Open a nganya profile with route, plate, crew rating, features, and live state.

### Crew Experience

- Toggle live tracking state for a vehicle.
- View current route assignment.
- View simple day metrics such as watchers, earnings, and fleet position.
- Trigger quick actions such as route update, pit stop, and crew chat as UI placeholders in the first pass.

### Fleet/Admin Experience

- See key fleet metrics.
- View active route status on a dashboard map.
- Review pending vehicle verifications.
- See simple crew performance and fleet distribution summaries.

## First Data Model

Keep the schema small and easy to evolve.

### `route`

- `id`
- `route_number`
- `name`
- `origin`
- `destination`
- `description`
- `active`
- `created_at`
- `updated_at`

### `vehicle`

- `id`
- `plate_number`
- `name`
- `route_id`
- `status`
- `occupancy_status`
- `verified`
- `wifi_available`
- `bass_level`
- `screen_count`
- `created_at`
- `updated_at`

### `crew_member`

- `id`
- `vehicle_id`
- `display_name`
- `role`
- `rating`
- `active`
- `created_at`
- `updated_at`

### `vehicle_location`

- `id`
- `vehicle_id`
- `latitude`
- `longitude`
- `speed_kph`
- `recorded_at`

### `fleet_alert`

- `id`
- `vehicle_id`
- `type`
- `message`
- `severity`
- `resolved`
- `created_at`
- `updated_at`

## API Surface

Use simple JSON APIs. Avoid nested workflows until the MVP is working.

### Public/Commuter APIs

- `GET /api/routes`
- `GET /api/routes/{id}`
- `GET /api/vehicles`
- `GET /api/vehicles/{id}`
- `GET /api/vehicles/nearby?lat={lat}&lng={lng}`
- `GET /api/vehicles/search?q={query}`

### Crew APIs

- `POST /api/crew/vehicles/{vehicleId}/go-live`
- `POST /api/crew/vehicles/{vehicleId}/go-offline`
- `POST /api/crew/vehicles/{vehicleId}/location`
- `PATCH /api/crew/vehicles/{vehicleId}/status`

### Fleet/Admin APIs

- `GET /api/admin/fleet/overview`
- `GET /api/admin/vehicles/pending-verification`
- `POST /api/admin/vehicles/{vehicleId}/verify`
- `GET /api/admin/alerts`
- `PATCH /api/admin/alerts/{alertId}/resolve`

## Frontend Structure

Recommended Angular structure:

- `core`: API clients, shared models, app-level services.
- `shared`: reusable UI pieces such as route chips, neon buttons, status badges, bottom nav, metric tiles.
- `features/live-map`: commuter live map and search flow.
- `features/nganya-profile`: vehicle profile screen.
- `features/crew-dashboard`: crew dashboard screen.
- `features/fleet-overview`: fleet/admin dashboard.

Initial routes:

- `/` -> live map/search
- `/nganyas/:id` -> nganya profile
- `/crew` -> crew dashboard
- `/fleet` -> fleet overview

## Implementation Phases

### Phase 1: Foundation

- Add Tailwind to Angular.
- Add the Nairobi Nights design tokens.
- Replace Angular starter page with the live map/search shell.
- Add Spring Boot package structure.
- Add Liquibase dependency and initial changelog.
- Configure PostgreSQL connection through environment variables.

### Phase 2: Core Data and APIs

- Add route, vehicle, crew member, vehicle location, and alert entities.
- Add repositories and simple services.
- Add read APIs for routes, vehicles, nearby vehicles, and search.
- Seed a small Nairobi-flavored demo dataset through Liquibase.

### Phase 3: Product Screens

- Build live map/search screen.
- Build nganya profile screen.
- Build crew dashboard screen.
- Build fleet overview screen.
- Wire screens to API clients with loading, empty, and error states.

### Phase 4: Tracking Loop

- Implement crew go-live/go-offline.
- Implement location updates.
- Show live state and latest location on commuter/admin views.
- Keep realtime behavior simple at first with polling. WebSockets can come later if needed.

### Phase 5: Hardening

- Add basic validation.
- Add focused backend service/controller tests.
- Add Angular component/service tests for API mapping and key UI states.
- Add production build checks.

## Non-Goals for the First Pass

- Complex authentication and permissions.
- Payment processing.
- Full realtime WebSocket infrastructure.
- Advanced route optimization.
- Native mobile packaging.
- Deep analytics or reporting.

## First Implementation Decision

Start with the foundation and demo data. This gives the frontend useful content immediately and keeps the backend centered on simple, testable APIs.
