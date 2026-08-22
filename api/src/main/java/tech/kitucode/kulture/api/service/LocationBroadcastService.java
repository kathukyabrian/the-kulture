package tech.kitucode.kulture.api.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.Vehicle;
import tech.kitucode.kulture.api.domain.VehicleLatestLocation;
import tech.kitucode.kulture.api.domain.enumerations.ListingState;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.repository.VehicleLatestLocationRepository;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.web.rest.dto.LocationBatchRequest;
import tech.kitucode.kulture.api.web.rest.dto.LocationBatchResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationSampleRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleLocationEvent;

@Service
@Slf4j
public class LocationBroadcastService {
	private static final Duration MAX_SAMPLE_AGE = Duration.ofHours(1);
	private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(2);
	private static final ZoneId API_TIME_ZONE = ZoneId.of("Africa/Nairobi");
	private final VehicleRepository vehicles;
	private final VehicleLatestLocationRepository latestLocations;
	private final ApplicationEventPublisher events;

	public LocationBroadcastService(VehicleRepository vehicles, VehicleLatestLocationRepository latestLocations, ApplicationEventPublisher events) {
		this.vehicles = vehicles;
		this.latestLocations = latestLocations;
		this.events = events;
	}

	@Transactional
	public LocationBatchResponse ingest(UUID vehicleId, LocationBatchRequest request) {
		Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
		if (vehicle.getStatus() != VehicleStatus.ONLINE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle must be online before sharing location");

		List<UUID> accepted = new ArrayList<>();
		List<UUID> duplicates = new ArrayList<>();
		List<LocationBatchResponse.LocationRejection> rejected = new ArrayList<>();
		VehicleLatestLocation latest = latestLocations.findById(vehicleId).orElseGet(() -> new VehicleLatestLocation(vehicle));
		Set<UUID> seenSampleIds = new HashSet<>();
		Instant now = Instant.now();

		for (LocationSampleRequest sample : request.samples()) {
			log.info("Location received vehicleId={} sessionId={} sampleId={} latitude={} longitude={} accuracyMeters={} speedKph={} headingDegrees={} recordedAt={}",
				vehicleId, request.sessionId(), sample.sampleId(), sample.latitude(), sample.longitude(), sample.accuracyMeters(), sample.speedKph(), sample.headingDegrees(), sample.recordedAt());
			if (!seenSampleIds.add(sample.sampleId()) || sample.sampleId().equals(latest.getSampleId())) { duplicates.add(sample.sampleId()); continue; }
			String reason = rejectionReason(sample, now, latest.getRecordedAt());
			if (reason != null) { rejected.add(new LocationBatchResponse.LocationRejection(sample.sampleId(), reason)); continue; }

			accepted.add(sample.sampleId());
			if (latest.getRecordedAt() == null || sample.recordedAt().isAfter(latest.getRecordedAt())) latest.update(sample.sampleId(), sample.latitude(), sample.longitude(), sample.speedKph(), sample.accuracyMeters(), sample.headingDegrees(), sample.recordedAt(), now);
		}

		if (latest.getRecordedAt() != null && accepted.contains(latest.getSampleId())) {
			latestLocations.save(latest);
			if (vehicle.isVerified() && vehicle.getListingState() == ListingState.ACTIVE) {
				events.publishEvent(new VehicleLocationEvent(latest.getSampleId(), vehicleId, latest.getLatitude(), latest.getLongitude(), latest.getSpeedKph(), latest.getRecordedAt().atZone(API_TIME_ZONE).toOffsetDateTime()));
			}
		}
		return new LocationBatchResponse(List.copyOf(accepted), List.copyOf(duplicates), List.copyOf(rejected));
	}

	private String rejectionReason(LocationSampleRequest sample, Instant now, Instant latestRecordedAt) {
		if (sample.accuracyMeters() != null && sample.accuracyMeters().doubleValue() > 50) return "accuracy exceeds 50 metres";
		if (sample.recordedAt().isBefore(now.minus(MAX_SAMPLE_AGE))) return "sample is too old";
		if (sample.recordedAt().isAfter(now.plus(MAX_CLOCK_SKEW))) return "sample timestamp is in the future";
		if (latestRecordedAt != null && !sample.recordedAt().isAfter(latestRecordedAt)) return "sample is older than the latest position";
		return null;
	}
}
