package tech.kitucode.kulture.api.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.Vehicle;
import tech.kitucode.kulture.api.domain.VehicleLatestLocation;
import tech.kitucode.kulture.api.domain.enumerations.ListingState;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.repository.VehicleLatestLocationRepository;
import tech.kitucode.kulture.api.web.rest.dto.ArrivalEstimateRequest;
import tech.kitucode.kulture.api.web.rest.dto.ArrivalEstimateResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ArrivalEstimateService {
	private static final double EARTH_RADIUS_METERS = 6_371_000;
	private final VehicleService vehicleService;
	private final VehicleLatestLocationRepository locations;
	private final ObjectMapper objectMapper;
	private final double corridorMeters;
	private final long freshnessSeconds;
	private final double averageSpeedKph;
	private final double maximumPassengerAccuracyMeters;

	public ArrivalEstimateService(VehicleService vehicleService, VehicleLatestLocationRepository locations, ObjectMapper objectMapper,
		@Value("${app.arrival-estimate.route-corridor-meters:300}") double corridorMeters,
		@Value("${app.arrival-estimate.vehicle-freshness-seconds:60}") long freshnessSeconds,
		@Value("${app.arrival-estimate.average-speed-kph:18}") double averageSpeedKph,
		@Value("${app.arrival-estimate.maximum-passenger-accuracy-meters:150}") double maximumPassengerAccuracyMeters) {
		this.vehicleService = vehicleService; this.locations = locations; this.objectMapper = objectMapper;
		this.corridorMeters = corridorMeters; this.freshnessSeconds = freshnessSeconds;
		this.averageSpeedKph = averageSpeedKph; this.maximumPassengerAccuracyMeters = maximumPassengerAccuracyMeters;
	}

	public ArrivalEstimateResponse estimate(UUID vehicleId, ArrivalEstimateRequest request) {
		Vehicle vehicle = vehicleService.findById(vehicleId);
		if (vehicle.getListingState() != ListingState.ACTIVE) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found");
		if (vehicle.getStatus() != VehicleStatus.ONLINE) return unavailable(vehicleId, "UNAVAILABLE", "Vehicle is offline or in maintenance", null);
		VehicleLatestLocation location = locations.findById(vehicleId).orElse(null);
		if (location == null || Duration.between(location.getRecordedAt(), Instant.now()).getSeconds() > freshnessSeconds)
			return unavailable(vehicleId, "STALE_LOCATION", "Live location is temporarily unavailable", location);
		if (request.accuracyMeters() != null && request.accuracyMeters().doubleValue() > maximumPassengerAccuracyMeters)
			return unavailable(vehicleId, "PASSENGER_LOCATION_UNRELIABLE", "Passenger location is not accurate enough", location);
		List<Point> route = readRoute(vehicle.getRoute().getGeometry());
		if (route.size() < 2) return unavailable(vehicleId, "ROUTE_UNAVAILABLE", "Route geometry is unavailable", location);
		Projection passenger = project(new Point(request.longitude().doubleValue(), request.latitude().doubleValue()), route);
		Projection nganya = project(new Point(location.getLongitude().doubleValue(), location.getLatitude().doubleValue()), route);
		if (passenger.distanceFromRoute > corridorMeters) return unavailable(vehicleId, "OFF_ROUTE", "Passenger is too far from this route", location);
		if (nganya.distanceFromRoute > corridorMeters) return unavailable(vehicleId, "ROUTE_UNAVAILABLE", "Vehicle could not be matched to its route", location);
		boolean forward = true;
		String confidence = "MEDIUM";
		if (location.getHeadingDegrees() != null && location.getSpeedKph() >= 5) {
			double difference = angleDifference(location.getHeadingDegrees().doubleValue(), nganya.segmentBearing);
			if (difference > 70 && difference < 110) return unavailable(vehicleId, "NOT_APPROACHING", "Vehicle direction is uncertain", location);
			forward = difference <= 90; confidence = "HIGH";
		}
		double remaining = forward ? passenger.progress - nganya.progress : nganya.progress - passenger.progress;
		if (remaining < -30) return unavailable(vehicleId, "PASSED", "Vehicle has passed the passenger position", location);
		if (remaining <= 30) remaining = 30;
		double baseMinutes = remaining / (averageSpeedKph * 1000 / 60);
		int min = Math.max(1, (int)Math.floor(baseMinutes * 0.8));
		int max = Math.max(min + 2, (int)Math.ceil(baseMinutes * 1.35 + 2));
		return new ArrivalEstimateResponse(vehicleId, "AVAILABLE", min, max, (int)Math.round(remaining), timestamp(location),
			Instant.now().atOffset(ZoneOffset.UTC), confidence, null, snapped(passenger), snapped(nganya));
	}

	public List<ArrivalEstimateResponse> estimateNearby(ArrivalEstimateRequest request) {
		return vehicleService.list().stream()
			.map(vehicle -> estimate(vehicle.id(), request))
			.sorted((left, right) -> Integer.compare(
				left.remainingDistanceMeters() == null ? Integer.MAX_VALUE : left.remainingDistanceMeters(),
				right.remainingDistanceMeters() == null ? Integer.MAX_VALUE : right.remainingDistanceMeters()))
			.toList();
	}

	private List<Point> readRoute(String geometry) {
		try { JsonNode coordinates = objectMapper.readTree(geometry).get("coordinates"); List<Point> points = new ArrayList<>();
			if (coordinates != null) coordinates.forEach(node -> points.add(new Point(node.get(0).asDouble(), node.get(1).asDouble()))); return points;
		} catch (Exception ignored) { return List.of(); }
	}
	private Projection project(Point point, List<Point> route) {
		Projection best = null; double progress = 0;
		for (int i = 0; i < route.size() - 1; i++) {
			Point a = route.get(i), b = route.get(i + 1); double midLat = Math.toRadians((a.lat + b.lat + point.lat) / 3);
			double x = Math.toRadians(b.lng-a.lng)*EARTH_RADIUS_METERS*Math.cos(midLat), y = Math.toRadians(b.lat-a.lat)*EARTH_RADIUS_METERS;
			double px = Math.toRadians(point.lng-a.lng)*EARTH_RADIUS_METERS*Math.cos(midLat), py = Math.toRadians(point.lat-a.lat)*EARTH_RADIUS_METERS;
			double length2=x*x+y*y, t=length2==0?0:Math.max(0,Math.min(1,(px*x+py*y)/length2));
			Point snap=new Point(a.lng+(b.lng-a.lng)*t,a.lat+(b.lat-a.lat)*t); double distance=distance(point,snap); double segment=Math.sqrt(length2);
			Projection candidate=new Projection(snap,distance,progress+segment*t,bearing(a,b)); if(best==null||distance<best.distanceFromRoute)best=candidate; progress+=segment;
		} return best;
	}
	private double distance(Point a, Point b) { double lat=Math.toRadians(b.lat-a.lat), lng=Math.toRadians(b.lng-a.lng), m=Math.toRadians((a.lat+b.lat)/2); return EARTH_RADIUS_METERS*Math.sqrt(lat*lat+lng*lng*Math.cos(m)*Math.cos(m)); }
	private double bearing(Point a, Point b) { double y=Math.sin(Math.toRadians(b.lng-a.lng))*Math.cos(Math.toRadians(b.lat)); double x=Math.cos(Math.toRadians(a.lat))*Math.sin(Math.toRadians(b.lat))-Math.sin(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))*Math.cos(Math.toRadians(b.lng-a.lng)); return (Math.toDegrees(Math.atan2(y,x))+360)%360; }
	private double angleDifference(double a,double b){ return Math.abs(((a-b+540)%360)-180); }
	private ArrivalEstimateResponse.SnappedPoint snapped(Projection p){ return new ArrivalEstimateResponse.SnappedPoint(p.point.lat,p.point.lng); }
	private java.time.OffsetDateTime timestamp(VehicleLatestLocation l){ return l==null?null:l.getRecordedAt().atOffset(ZoneOffset.UTC); }
	private ArrivalEstimateResponse unavailable(UUID id,String status,String reason,VehicleLatestLocation l){ return new ArrivalEstimateResponse(id,status,null,null,null,timestamp(l),Instant.now().atOffset(ZoneOffset.UTC),"LOW",reason,null,null); }
	private record Point(double lng,double lat){} private record Projection(Point point,double distanceFromRoute,double progress,double segmentBearing){}
}
