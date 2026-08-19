# Maps and live route tracking

## Objective

The Kulture needs a map that can:

- Display a matatu route from stored coordinates.
- Display the latest location of each nganya on that route.
- Update a nganya marker as new coordinates arrive.
- Allow administrators to create or edit route geometry.
- Eventually determine whether a nganya is on its assigned route and how far it has progressed.

## Technology decision

**The Kulture will use MapLibre GL JS as its web map renderer.**

This is a committed project decision. New map screens should use MapLibre rather than introducing Leaflet, OpenLayers, Google Maps or Mapbox GL JS. Shared MapLibre services and components should be reused across public, traveller, crew and admin experiences so map behavior remains consistent.

The tile provider remains configurable and is a separate decision from the renderer.

## Selected stack

Use the following stack for the initial implementation:

```text
Angular
  → MapLibre GL JS
  → hosted OpenStreetMap-compatible vector tiles

Spring Boot
  → route geometry API
  → latest vehicle location API
  → polling initially, WebSocket or SSE later

Database
  → GeoJSON initially
  → PostgreSQL with PostGIS when spatial queries are required
```

MapLibre GL JS is an open-source TypeScript/WebGL library with support for vector tiles, GeoJSON lines, markers, custom icons, popups and real-time source updates. Its official examples include GeoJSON line rendering and live data updates.

- [MapLibre GL JS documentation](https://maplibre.org/maplibre-gl-js/docs)
- [MapLibre examples](https://maplibre.org/maplibre-gl-js/docs/examples/)

## Why MapLibre GL JS

MapLibre was selected because it provides:

- Smooth WebGL rendering.
- Custom map styles that can match the application design.
- GeoJSON line and point layers.
- Custom nganya icons and popups.
- Efficient rendering of multiple vehicles.
- Animated source updates.
- A path toward vector tiles, rotation and mobile-native MapLibre clients.
- No dependency on a proprietary map renderer.

Leaflet was not selected because the planned experience needs richer vector styling and live vehicle visualization. OpenLayers was not selected because its broader GIS capabilities add complexity beyond the immediate product requirements. These libraries should not be added alongside MapLibre unless this decision is deliberately revisited.

## Angular integration

Install MapLibre directly rather than depending on an Angular wrapper:

```bash
npm install maplibre-gl
```

Import the renderer and its required stylesheet:

```typescript
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
```

Map instances should be created after the view container exists and removed in `ngOnDestroy`:

```typescript
private map?: maplibregl.Map;

ngAfterViewInit(): void {
  this.map = new maplibregl.Map({
    container: this.mapContainer.nativeElement,
    style: environment.mapStyleUrl,
    center: [36.8219, -1.2921],
    zoom: 11
  });
}

ngOnDestroy(): void {
  this.map?.remove();
}
```

Keep the style URL in environment/configuration rather than hard-coding it. Application components should receive route and vehicle data through existing Angular services; they should not fetch backend data from inside generic map-rendering utilities.

Create reusable map building blocks instead of initializing unrelated maps independently:

- A shared MapLibre map host responsible for lifecycle and resize handling.
- A route-line layer that accepts GeoJSON `LineString` data.
- A vehicle layer that accepts a GeoJSON `FeatureCollection<Point>`.
- Shared popup and nganya-marker presentation.
- A viewport helper that fits the selected route and visible vehicles.
- An admin drawing adapter, using Terra Draw when route editing is implemented.

## Route geometry

Store each route as a GeoJSON `LineString`:

```json
{
  "type": "LineString",
  "coordinates": [
    [36.8219, -1.2921],
    [36.8351, -1.2784],
    [36.8572, -1.2633]
  ]
}
```

GeoJSON coordinates use `[longitude, latitude]`. This is the reverse of the `[latitude, longitude]` order commonly shown in user interfaces and the browser geolocation API.

For the initial implementation, add a nullable route geometry field:

```text
routes
- id
- route_number
- name
- origin
- destination
- description
- geometry (JSON/JSONB)
```

The API should return geometry as valid GeoJSON rather than exposing database-specific spatial representations.

When PostGIS is introduced, migrate the value to a geometry column such as:

```text
geometry(LineString, 4326)
```

## Route creation

Two route-creation approaches are available.

### Manual route drawing

An administrator clicks points on the map to construct the route line. The editor should support:

- Adding a point.
- Dragging or removing a point.
- Undoing the last change.
- Clearing the route.
- Previewing the final line.
- Saving the result as GeoJSON.

Manual drawing is valuable for matatu routes because the actual route may use specific stages, turns and deviations that a generic origin-to-destination router would not select.

MapLibre will be paired with Terra Draw for interactive route drawing, modification and snapping workflows.

### Generated road route

An administrator supplies an origin, destination and optional waypoints. A routing engine returns a road-following line. The administrator should still review and edit the result before saving it.

Open-source routing engines include:

- **OSRM** — fast road routing with a relatively focused API.
- **GraphHopper** — a Java routing engine that can run as a library or standalone service.
- **Valhalla** — multimodal routing and map matching using OpenStreetMap and optional GTFS data.

References:

- [GraphHopper repository and documentation](https://github.com/graphhopper/graphhopper)
- [Valhalla API documentation](https://valhalla.github.io/valhalla/api/)
- [Valhalla repository](https://github.com/valhalla/valhalla)

For The Kulture, start with manual drawing and add optional road snapping or generated routes later.

## Live nganya locations

The existing `vehicle_locations` data remains the source of vehicle positions. Each location contains:

- Vehicle ID.
- Latitude.
- Longitude.
- Speed.
- Recorded timestamp.

The map should render the selected route as a line and the latest location as a point or custom nganya marker.

Initial update flow:

```text
Crew device geolocation
  → POST latest coordinate
  → Spring Boot stores vehicle location
  → traveller map polls latest locations
  → MapLibre updates the vehicle GeoJSON source
```

Polling every 15–30 seconds is sufficient initially because the crew client already throttles location submissions. Later, Server-Sent Events or WebSockets can push updates to viewers and reduce unnecessary requests.

Each displayed location should include freshness information. A location older than the configured threshold, currently five minutes in the crew UI, should be marked stale rather than presented as live.

## On-route checks

Displaying a marker on top of a route does not prove the vehicle is following that route. Proper validation requires measuring the distance from the vehicle point to the route line.

With PostGIS, use spatial operations such as:

```sql
ST_DWithin(vehicle_position, route_geometry, allowed_distance)
```

The allowed distance should account for GPS accuracy, divided roads and minor route deviations. Start with a configurable threshold between 100 and 250 metres and evaluate it using real Nairobi location data.

PostGIS can also calculate route progress:

```sql
ST_LineLocatePoint(route_geometry, vehicle_position)
```

This returns a position along the line between `0` and `1`. It can support features such as:

- “Baba Yaga is 63% along Route 125.”
- Distance remaining to the destination.
- Ordered vehicle positions along a route.
- Off-route alerts.

For noisy GPS traces, a map-matching engine such as Valhalla can snap a sequence of recorded positions to the road network.

## Tiles and OpenStreetMap

MapLibre is a renderer. It does not provide production basemap infrastructure by itself.

Initial options include:

- Use a hosted OpenStreetMap-compatible tile provider.
- Use hosted vector tiles with MapLibre.
- Self-host an OpenMapTiles-compatible stack when traffic or offline requirements justify it.
- Operate a dedicated OpenStreetMap tile server at larger scale.

Do not build a production dependency around the public `tile.openstreetmap.org` service. OpenStreetMap data is open, but the public tile servers have limited capacity, no SLA, prohibit bulk downloading and may block applications that violate the policy.

- [OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/)

The tile URL and map style URL should be configuration values so providers can be changed without an application release.

Visible OpenStreetMap attribution must be retained wherever OSM-derived data or tiles require it.

## Suggested API changes

Extend route responses with geometry:

```json
{
  "id": "uuid",
  "routeNumber": "125",
  "name": "Rongai",
  "origin": "CBD",
  "destination": "Rongai",
  "geometry": {
    "type": "LineString",
    "coordinates": []
  }
}
```

Add or extend admin operations:

- `PUT /api/admin/routes/{routeId}` — save route metadata and validated geometry.
- Validate that geometry is a `LineString`, contains at least two distinct points and uses valid WGS84 coordinate ranges.

Public location operations can either remain part of vehicle summaries or be separated for efficient polling:

- `GET /api/vehicles?routeId={routeId}` — public vehicles and latest locations for a route.
- `GET /api/routes/{routeId}` — public route metadata and geometry.
- Future: `GET /api/routes/{routeId}/live` using SSE for live updates.

Only crew assigned to a vehicle may submit its coordinates. Public users may read only vehicles with public/active listings.

## Delivery phases

### Phase 1: real map

1. Add MapLibre GL JS to Angular.
2. Configure a development tile/style provider.
3. Replace the decorative map background with a real map.
4. Display current nganya markers from existing location data.
5. Preserve stale-location indicators and public listing rules.

### Phase 2: route geometry

1. Add route geometry storage and API validation.
2. Add an admin route drawing interface.
3. Display route lines on public, traveller and crew maps.
4. Fit the viewport to the selected route and its vehicles.

### Phase 3: live updates

1. Poll location changes initially.
2. Animate markers between valid updates.
3. Add SSE or WebSocket delivery when scale requires it.
4. Handle disconnected, stale and inaccurate devices explicitly.

### Phase 4: spatial intelligence

1. Enable PostgreSQL/PostGIS.
2. Add on-route distance checks.
3. Add progress-along-route calculations.
4. Add map matching if raw GPS quality requires it.

## Acceptance criteria

- A route can store and return valid GeoJSON line coordinates.
- An admin can create, review and edit route geometry on a map.
- Public and authenticated users can see the selected route line.
- Each public nganya with a location appears at its latest coordinate.
- New coordinates update the marker without rebuilding the entire map.
- Stale locations are clearly identified.
- Hidden and suspended nganyas never appear on the public map.
- The system preserves required map-data attribution.
- Tile and style providers are configurable.
- Crew location submission remains assignment-authorized.
- Later PostGIS adoption does not require changing the public GeoJSON contract.
