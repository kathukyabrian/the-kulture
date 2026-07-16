package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LocationResponse(
	BigDecimal latitude,
	BigDecimal longitude,
	int speedKph,
	Instant recordedAt
) {
}
