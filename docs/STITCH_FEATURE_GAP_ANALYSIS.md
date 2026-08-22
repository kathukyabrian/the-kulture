# Stitch Feature Gap Analysis

This document compares the current Kulture implementation with the screens in `stitch_nganya_live_tracker.zip`. It separates missing product functionality from visual or navigation differences.

## Summary

The core live-tracking experience is substantially implemented. The largest remaining functional gaps are saved vehicles, proper trip and stop progression, crew communication tools, and admin alerts and analytics.

## Missing or Incomplete Features

### Traveller and Live Map

- **Saved or favourite nganyas:** The vehicle profile includes a Save action in Stitch, but there is no complete favourite-vehicle feature.
- **Track Now journey mode:** Stitch suggests that a traveller can select a vehicle and continue following it. The current map displays live vehicles but does not provide a dedicated tracking session for one vehicle.
- **Current stage or stop:** Nearby vehicle cards should show the vehicle's current stage or next stop.
- **Route-progress ETA:** The current ETA is an estimate and is not calculated from route stops and actual trip progress.
- **Vehicle navigation action:** Stitch provides a navigation action on each nearby vehicle card. This could open directions from the traveller's location to the vehicle or an appropriate pickup stage.
- **Nearby vehicle thumbnails and ratings:** These appear in the Stitch cards but are not fully represented in the current nearby-vehicle list.

### Crew Dashboard

- **Update active route:** Crew members should be able to change the route currently assigned to their active journey.
- **Crew chat or dispatch messaging:** The Crew Chat quick action shown in Stitch is not implemented.
- **Real commuter or watcher count:** A watcher count exists in the data model, but a complete real-time counting workflow is not evident.
- **Readable current location:** Stitch shows a named current location. The current implementation primarily works with map coordinates and would require reverse geocoding for a street or area name.
- **Fleet position presentation:** Fleet position is available in the model but is not presented as prominently as in Stitch.

### Admin Fleet Overview

- **Live operational alerts:** Stitch includes alerts such as critical speed violations. There is no complete alert-generation and resolution workflow.
- **Real live-route status map:** The dashboard map is primarily presentational and should be connected to actual fleet location data.
- **Reject or dismiss verification:** Pending vehicles can be verified, but Stitch also shows a rejection or dismissal action.
- **Top crew performance:** There is no complete crew ranking table based on rating and operational performance.
- **Accurate fleet distribution:** Active, idle and maintenance totals should be calculated from real vehicle states.
- **Revenue reporting:** Stitch shows fleet revenue and crew daily earnings. This is currently absent and may be intentional because earnings-related storage was previously removed.

## Features Already Covered

- Live vehicle plotting and location updates.
- Vehicle and route search.
- My-location map control.
- Vehicle profiles.
- Vehicle image galleries and custom features.
- Crew information and ratings.
- Crew online and location-sharing controls.
- Maintenance or pit-stop state.
- Vehicle verification.
- Nearby vehicle status and occupancy.
- Route geometry using OpenRouteService.

## Design-Parity Differences

These differences do not necessarily represent missing product functionality:

- Stitch uses a consistent four-tab bottom navigation: Map, Fleet, Crew and Profile.
- The current search updates immediately rather than requiring a separate GO button.
- Card layouts, thumbnails, map markers and floating controls differ visually from Stitch.
- Some Stitch dashboard metrics are positioned as prominent summary cards while the current application exposes related information elsewhere.

## Recommended Priority

1. Add saved or favourite nganyas.
2. Add a dedicated Track Now mode for following one vehicle.
3. Model route stops and calculate current stop, next stop and route-progress ETA.
4. Add crew route updates and crew or dispatch messaging.
5. Replace the admin dashboard's presentational map and distribution data with live fleet data.
6. Add operational alerts and vehicle verification rejection.
7. Decide whether revenue and earnings belong in the product before rebuilding those features.

## Relevant Implementation Areas

- `app/src/app/features/live-map/`
- `app/src/app/features/nganya-profile/`
- `app/src/app/features/crew-dashboard/`
- `app/src/app/features/admin-dashboard-page/`
- `docs/stitch_nganya_live_tracker.zip`
