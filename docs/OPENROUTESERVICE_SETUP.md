# openrouteservice setup

Road-following route creation uses the hosted openrouteservice API. No local map dataset or routing container is required.

1. Create an API key at <https://openrouteservice.org/dev/>.
2. Set it on the deployment host without committing it:

   ```bash
   export APP_ROUTING_ORS_API_KEY=your-api-key
   ```

3. Recreate the API container so Docker Compose passes the key through:

   ```bash
   docker compose up -d --force-recreate api
   ```

The Angular application never receives the key. Admin route calculations call the Spring endpoint, which calls `https://api.openrouteservice.org/v2/directions/driving-car/geojson`.

New routes follow roads after the second control point is selected. Existing routes remain unchanged until an admin opens the route, clicks **Follow roads**, reviews the preview, and saves it.
