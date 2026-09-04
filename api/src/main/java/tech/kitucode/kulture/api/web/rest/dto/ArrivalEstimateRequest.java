package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ArrivalEstimateRequest(
	@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
	@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
	@DecimalMin("0") BigDecimal accuracyMeters,
	Long capturedAtEpochMillis
) {}
