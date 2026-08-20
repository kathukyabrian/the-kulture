package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LocationResponse(
	BigDecimal latitude,
	BigDecimal longitude,
	int speedKph,
	OffsetDateTime recordedAt
) {
}
