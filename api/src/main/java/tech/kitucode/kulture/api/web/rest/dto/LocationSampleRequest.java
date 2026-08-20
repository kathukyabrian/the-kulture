package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LocationSampleRequest(
	@NotNull UUID sampleId,
	@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
	@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
	@DecimalMin("0.0") @DecimalMax("1000.0") BigDecimal accuracyMeters,
	@Min(0) @Max(160) int speedKph,
	@DecimalMin("0.0") @DecimalMax("360.0") BigDecimal headingDegrees,
	@NotNull Instant recordedAt
) {}
