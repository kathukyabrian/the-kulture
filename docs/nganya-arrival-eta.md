# Nganya-to-passenger arrival estimate

## Goal

Show a passenger how long the nganya they are currently viewing is expected to take to reach them.

This is a single-nganya feature. It belongs on the nganya profile page after a passenger selects one vehicle. It must not calculate or display passenger-specific ETAs on the main live map, search results, or nganya lists.

## User experience

### Entry point

The existing map and lists continue to show vehicle availability and live position without a passenger-specific ETA. Selecting a nganya opens its profile. In the profile's **Live location** section, the passenger can choose **Estimate arrival to me**.

Location access should be requested only after that action. It should not be requested merely because the user opened the main map or browsed a list.

### Successful state

The profile map shows:

- The selected nganya.
- The passenger's pickup position.
- The relevant section of the route between them.
- A primary estimate such as **8–12 min away**.
- Remaining route distance and freshness, for example **2.7 km · updated 9 sec ago**.

A range communicates uncertainty more honestly than an exact minute. The estimate should update while the profile remains open.

### Other states

The interface needs explicit states rather than showing a misleading number:

| Condition | Suggested message |
| --- | --- |
| Location permission has not been requested | Estimate arrival to me |
| Permission denied | Allow location access to estimate arrival |
| Passenger is too far from the nganya's route | Move closer to this route to get an estimate |
| Nganya has already passed the passenger | This nganya has passed your position |
| Nganya is travelling in the opposite direction | Not currently approaching you |
| Vehicle GPS is older than the freshness threshold | Live location is temporarily unavailable |
| Vehicle is offline or in maintenance | Arrival estimate unavailable |
| Route or routing service is unavailable | Could not calculate an estimate right now |
| Calculation is in progress | Estimating arrival… |

## Scope and visibility

The calculation starts when all of the following are true:

1. A passenger is viewing one nganya profile.
2. The passenger explicitly enables the estimate.
3. The selected vehicle is online and has a fresh location.
4. The browser or device has supplied the passenger's location.

Only the selected vehicle is calculated. Leaving the profile stops passenger-location watching and ETA refreshes.

The feature should appear on the public and traveller versions of the profile. Admin and crew preview pages should not request the viewer's location or display a passenger ETA by default.

## Existing foundation

The application already has:

- A single-nganya profile and live map.
- Stored route geometry as a `LineString`.
- A latest vehicle location containing latitude, longitude, speed, heading, and recording time.
- Five-second profile refreshes.
- Live vehicle-location events elsewhere in the application.
- Browser/device geolocation support in the shared map.
- An `etaMinutes` field on vehicle summaries.

The existing `etaMinutes` value is not sufficient for this feature because an arrival-to-passenger estimate depends on the current passenger location. The nganya detail response also does not currently contain a passenger-specific estimate.

## Calculation model

### 1. Capture the passenger position

After the passenger opts in, start a location watch while the profile is open. Keep latitude, longitude, accuracy, and capture time. Reject or clearly qualify readings that are too old or too inaccurate.

The passenger coordinate should be treated as an approximate pickup point. For privacy, it should not be permanently stored unless a separate product requirement and consent flow are introduced.

### 2. Match both positions to the route

Snap the vehicle and passenger coordinates to the selected nganya's route geometry. Record each point's distance from the start of the route.

This establishes route order. Straight-line distance must not be used as the arrival distance because it ignores bends, junctions, one-way roads, and the actual route.

If the passenger is outside a defined corridor around the route, no arrival estimate should be shown. The corridor should initially be configurable rather than hard-coded; a starting value can be validated against real route geometry and GPS noise.

### 3. Establish direction

Determine which direction the nganya is travelling using, in order of confidence:

1. Progress across recent map-matched vehicle samples.
2. Vehicle heading while it is moving at a reliable speed.
3. The vehicle's declared destination or active trip direction.

Direction matters because the same physical route may serve both inbound and outbound vehicles. If the passenger is behind the nganya in its current direction, report that it has passed instead of estimating a circular or reverse journey.

For dependable behaviour, the backend should eventually model a vehicle's active trip direction explicitly. A destination name alone can be ambiguous.

### 4. Calculate road travel time

For the first production-quality version, ask a road-routing service for driving duration from the map-matched vehicle position to the map-matched pickup point, constrained with route waypoints where necessary.

The duration should then be adjusted for typical nganya delays, including:

- Stops for boarding and alighting.
- Terminus dwell time when applicable.
- Recent observed speed.
- Time of day and day of week.
- GPS and routing uncertainty.

A traffic-aware provider can improve the initial estimate. A non-traffic routing engine can still support an MVP when combined with conservative ranges and route-specific average speeds.

### 5. Return a range and confidence

The service should return a lower and upper estimate rather than only one integer. It should also return remaining distance, calculation time, and a confidence level based on location freshness, GPS accuracy, direction certainty, and traffic-data quality.

## Backend responsibility

ETA calculation should live on the backend because it:

- Keeps routing credentials out of the client.
- Applies one consistent map-matching and direction policy.
- Allows routing results to be cached.
- Controls provider cost and rate limits.
- Provides a place to incorporate historical route performance later.

Conceptually, the client sends the selected vehicle identifier and the passenger's current coordinate. The response contains:

- Vehicle identifier.
- Estimate status.
- Minimum and maximum minutes.
- Remaining distance in metres.
- Vehicle-location timestamp.
- Estimate timestamp.
- Confidence level.
- Optional reason when no estimate is available.

The backend must verify that the requested vehicle is publicly visible and should avoid logging precise passenger coordinates in ordinary request logs.

## Refresh and caching policy

The client should not request routing on every raw GPS event. Recalculate when any of these occurs:

- The vehicle has moved a meaningful distance, initially around 100–200 metres.
- A bounded refresh interval has elapsed, initially around 15–30 seconds.
- The passenger has moved far enough to change the pickup point materially.
- The selected vehicle's direction changes.

Cache estimates briefly using the vehicle, route direction, and coarse pickup area. Invalidate the cache when the vehicle advances materially. If a refresh fails, the last estimate may remain visible only for a short period and must show its age.

The current five-second profile polling can continue to refresh vehicle details, but it should not automatically cause a paid routing request every five seconds. Adding the existing vehicle-location event stream to the profile can make marker motion smoother independently of the ETA cadence.

## Accuracy and safety rules

- Do not calculate from a stale vehicle location. The application currently treats locations older than 60 seconds as stale; retain that as an initial upper bound and tune it with field data.
- Ignore unreliable heading when the vehicle is stationary or moving very slowly.
- Smooth short-term GPS jumps before map matching.
- Never show a negative ETA.
- Suppress an estimate when route direction is uncertain.
- Distinguish **arriving near your route position** from door-to-door arrival; the nganya remains on its registered route.
- Do not promise an exact pickup time or that the vehicle will stop.

## Delivery phases

### Phase 1: Route-based MVP

- Passenger opts in on one nganya profile.
- Both positions are map-matched to stored route geometry.
- Direction and passed-vehicle checks are enforced.
- Remaining route distance is divided by a conservative route-specific average speed.
- The UI shows a broad time range and location freshness.

This validates the interaction without requiring high routing-provider usage.

### Phase 2: Road-routing estimate

- Replace or refine average-speed duration with road-routing duration.
- Add stop-time adjustment, caching, and confidence scoring.
- Measure estimated versus actual arrival error.

### Phase 3: Historical prediction

- Learn travel times from consenting fleet location samples.
- Segment observations by route, direction, road section, weekday, and time band.
- Blend historical speeds, current vehicle movement, and live traffic where available.
- Tune the displayed interval from measured prediction error.

## Acceptance criteria

- No passenger-specific ETA appears or is calculated on the main map or any nganya list.
- Location permission is requested only from the single-nganya profile after a passenger action.
- Only the selected nganya is included in the ETA request.
- An online, approaching nganya with fresh GPS data produces an ETA range and remaining distance.
- Passed, opposite-direction, offline, stale, and off-route cases display a clear state instead of a number.
- The estimate visibly states when it was last updated.
- Closing or leaving the profile stops location watching and ETA refreshes.
- Admin and crew previews do not request passenger location.
- Precise passenger coordinates are not retained or exposed to other users.
- Routing requests are rate-limited and are not coupled one-for-one to GPS updates.

## Decisions still required

- Which routing provider or self-hosted routing engine to use.
- Whether anonymous public visitors can request estimates or only signed-in travellers.
- The acceptable passenger-to-route corridor for each route type.
- The GPS freshness and accuracy thresholds after field testing.
- Whether the pickup position can be adjusted manually when device GPS is poor.
- Target accuracy, latency, and monthly routing budget.
