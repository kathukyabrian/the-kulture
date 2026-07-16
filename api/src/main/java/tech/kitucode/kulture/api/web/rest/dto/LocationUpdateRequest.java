package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LocationUpdateRequest(
	@NotNull BigDecimal latitude,
	@NotNull BigDecimal longitude,
	@Min(0) @Max(160) int speedKph
) {
}
