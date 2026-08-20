package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleLocationEvent(UUID sampleId, UUID vehicleId, BigDecimal latitude, BigDecimal longitude, int speedKph, OffsetDateTime recordedAt) {}
